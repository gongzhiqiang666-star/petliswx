package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Signed, actor-bound preview. Commit locks all existing rows before any write. */
@Service
public class LegacyPaymentLogImportService {
 private final FeatureStore store;private final AccessService access;private final LegacyPaymentLogService logs;private final byte[] signingKey;
 public LegacyPaymentLogImportService(FeatureStore store,AccessService access,LegacyPaymentLogService logs,@Value("${app.jwt.secret}") String secret){this.store=store;this.access=access;this.logs=logs;this.signingKey=secret.getBytes(StandardCharsets.UTF_8);}
 public record PreparedRow(int rowNumber,Long id,Map<String,String> fields,String expectedToken){}
 public record Preview(List<PreparedRow> rows,int created,int updated,List<String> warnings,String encoding,long expiresAt,String previewToken){}
 public record ImportCommand(List<PreparedRow> rows,long expiresAt,String previewToken,String requestKey){}
 public record ImportResult(List<Long> ids,int created,int updated){}
 private void policy(boolean lock){if(!logs.policy(lock)[3])throw new BusinessException(409,"交易流水导入尚未开启");}
 private String sign(String purpose,Object value){try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(signingKey,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(("petlis-payment-import-v1:"+purpose+":"+store.encode(value)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("流水导入校验失败");}}
 private Map<String,Object> payload(int actor,List<PreparedRow> rows,long expiresAt){var canonical=new TreeMap<String,Object>();canonical.put("actor",actor);canonical.put("expiresAt",expiresAt);var list=new ArrayList<Map<String,Object>>();for(var r:rows){var item=new TreeMap<String,Object>();item.put("rowNumber",r.rowNumber());item.put("id",r.id());item.put("fields",new TreeMap<>(r.fields()));item.put("expectedToken",r.expectedToken());list.add(item);}canonical.put("rows",list);return canonical;}
 @Transactional(readOnly=true) public byte[] template(){policy(false);return ("\ufeff"+String.join(",",LegacyPaymentLogService.FIELDS)+"\r\n").getBytes(StandardCharsets.UTF_8);}
 @Transactional(readOnly=true) public Preview preview(String filename,byte[] bytes,String encoding,String headerType){
  policy(false);if(!List.of("name","comment").contains(headerType==null?"":headerType))throw new BusinessException(400,"请选择字段名或原注释表头");
  var file=LegacyPaymentLogImportFile.read(filename,bytes,encoding);var warnings=new LinkedHashSet<String>();var headers=new HashMap<String,String>();
  if(headerType.equals("name")){for(String f:LegacyPaymentLogService.FIELDS)headers.put(f,f);headers.put("PayLogId","PayLogId");}
  else{for(var row:store.jdbc().queryForList("SELECT COLUMN_NAME,COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_paylog' ORDER BY ORDINAL_POSITION"))headers.put(row.get("COLUMN_COMMENT").toString(),row.get("COLUMN_NAME").toString());}
  var columns=new LinkedHashMap<String,Integer>();for(int i=0;i<file.headers().size();i++){String header=file.headers().get(i),field=headers.get(header);if(field==null){warnings.add("忽略未知列："+(header.isEmpty()?"空表头":header));continue;}if(columns.containsKey(field))warnings.add("重复表头按PHP规则使用最后一列："+field);columns.put(field,i);}
  if(columns.isEmpty())throw new BusinessException(400,"表头未匹配原业务字段，请检查表头类型");if(file.rows().isEmpty())throw new BusinessException(400,"没有可导入的记录");
  var prepared=new ArrayList<PreparedRow>();var virtual=new HashMap<Long,Map<String,String>>();var versions=new HashMap<Long,String>();var transactionIds=new HashSet<String>();int created=0,updated=0;
  for(var input:file.rows()){
   try{
    var patch=new LinkedHashMap<String,String>();for(var column:columns.entrySet())patch.put(column.getKey(),column.getValue()<input.cells().size()?input.cells().get(column.getValue()):"");
    Long id=null;var fields=new LinkedHashMap<String,String>();String expected=null;
    if(patch.containsKey("PayLogId")){String pk=patch.remove("PayLogId");if(!pk.matches("[0-9]{1,10}")||(id=Long.valueOf(pk))<1||id>4294967295L)throw new BusinessException(400,"更新文件的PayLogId必须为有效主键，新增文件请去掉主键列");
     if(!virtual.containsKey(id)){var raw=logs.raw(id,false);var display=logs.display(raw);var existing=new TreeMap<String,String>();for(String f:LegacyPaymentLogService.FIELDS)existing.put(f,Objects.toString(display.get(f),""));virtual.put(id,existing);versions.put(id,logs.token(raw));}
     fields.putAll(virtual.get(id));expected=versions.get(id);updated++;
    }else created++;
    fields.putAll(patch);
    String money=fields.get("TotalFee");if(money!=null){String value=money.trim();if(!value.matches("[+-]?[0-9]{1,18}(\\.[0-9]{1,100})?"))throw new BusinessException(400,"交易金额不是有效十进制数");String rounded=new BigDecimal(value).setScale(2,RoundingMode.HALF_UP).toPlainString();fields.put("TotalFee",rounded);if(new BigDecimal(value).compareTo(new BigDecimal(rounded))!=0)warnings.add("第"+input.number()+"行交易金额按原字段四舍五入为两位小数");}
    var normalized=logs.normalized(fields);if(id!=null)virtual.put(id,normalized);
    String txn=normalized.get("TradeNoAli");if(!txn.isEmpty()&&(!transactionIds.add(txn)||store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoAli=?"+(id==null?"":" AND PayLogId<>?"),Long.class,id==null?new Object[]{txn}:new Object[]{txn,id})>0))warnings.add("第"+input.number()+"行平台流水号有重复记录，将按原规则保留，不自动去重");
    prepared.add(new PreparedRow(input.number(),id,normalized,expected));
   }catch(BusinessException e){throw new BusinessException(e.getCode()==404?400:e.getCode(),"第"+input.number()+"行："+e.getMessage());}
  }
  long expires=Instant.now().getEpochSecond()+1800;return new Preview(List.copyOf(prepared),created,updated,List.copyOf(warnings),file.encoding(),expires,sign("preview",payload(access.current().userId(),prepared,expires)));
 }
 @Transactional public ImportResult commit(ImportCommand command){
  policy(true);if(command==null||command.rows()==null||command.rows().isEmpty()||command.rows().size()>LegacyPaymentLogImportFile.MAX_ROWS||command.previewToken()==null||!command.previewToken().matches("[a-f0-9]{64}")||command.requestKey()==null||!command.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"导入请求不完整，请重新预览");
  for(var row:command.rows()){if(row==null||row.fields()==null||row.rowNumber()<2)throw new BusinessException(400,"导入预览行无效");logs.normalized(row.fields());}
  int actor=access.current().userId();String fingerprint=sign("preview",payload(actor,command.rows(),command.expiresAt()));if(!MessageDigest.isEqual(fingerprint.getBytes(StandardCharsets.US_ASCII),command.previewToken().getBytes(StandardCharsets.US_ASCII)))throw new BusinessException(409,"导入预览已改变或不属于当前账号，请重新预览");
  store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_PAYMENT_LOG_IMPORT' AND request_id=? ORDER BY id DESC LIMIT 1",actor,command.requestKey());
  if(!prior.isEmpty()){try{var mapper=new ObjectMapper();var data=mapper.readTree(prior.get(0).get("after_json").toString());if(!fingerprint.equals(data.path("requestFingerprint").asText()))throw new BusinessException(409,"请求号已用于其他导入");return mapper.treeToValue(data.path("result"),ImportResult.class);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"导入结果无法核对");}}
  if(command.expiresAt()<Instant.now().getEpochSecond())throw new BusinessException(409,"导入预览已过期，请重新预览");
  var expected=new TreeMap<Long,String>();for(var row:command.rows()){logs.normalized(row.fields());if(row.id()!=null){if(row.expectedToken()==null||expected.containsKey(row.id())&&!expected.get(row.id()).equals(row.expectedToken()))throw new BusinessException(400,"更新行版本无效");expected.put(row.id(),row.expectedToken());}}
  for(var entry:expected.entrySet())if(!logs.token(logs.raw(entry.getKey(),true)).equals(entry.getValue()))throw new BusinessException(409,"待导入流水已被修改，请重新预览；本批次未写入");
  var ids=new ArrayList<Long>();int created=0,updated=0,index=0;
  for(var row:command.rows()){String version=row.id()==null?null:logs.detail(row.id()).get("editToken").toString();String rowKey=sign("row-request",List.of(actor,command.requestKey(),index++));ids.add(logs.save(row.id(),new LegacyPaymentLogService.Command(row.fields(),rowKey,version)));if(row.id()==null)created++;else updated++;}
  var result=new ImportResult(List.copyOf(ids),created,updated);store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,'LEGACY_PAYMENT_LOG_IMPORTED','LEGACY_PAYMENT_LOG_IMPORT','batch',?,?)",actor,command.requestKey(),store.encode(Map.of("requestFingerprint",fingerprint,"result",result)));return result;
 }
}
