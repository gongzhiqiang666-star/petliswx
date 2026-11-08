package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
/** finance/Project index/multi/writeoff; approved single-save deviation preserves existing bodies. */
@Service @RequiredArgsConstructor
public class LegacyFinanceService {
 private final FeatureStore store;private final AccessService access;
 public record WriteoffCommand(String type,String requestKey,String expectedToken){}
 public record Target(long id,String editToken){}
 public record BatchCommand(List<Target> targets,String field,int value,String requestKey){}
 private boolean[] policy(boolean lock){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-finance' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));if(rows.size()!=1)return new boolean[]{false,false,false};try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=j.path("enabled").isBoolean()&&j.path("enabled").asBoolean();return new boolean[]{read,read&&j.path("writeEnabled").isBoolean()&&j.path("writeEnabled").asBoolean(),read&&j.path("writeEnabled").isBoolean()&&j.path("writeEnabled").asBoolean()&&j.path("singleWriteoffEnabled").isBoolean()&&j.path("singleWriteoffEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"原财务参数无效");}}
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("legacyFinance",p[0],"legacyFinanceWrites",p[1],"legacySingleWriteoff",p[2]);}
 private void read(){if(!policy(false)[0])throw new BusinessException(409,"原财务明细尚未开启");}
 private Map<String,Object> raw(long id,boolean lock){if(id<1||id>4294967295L)throw new BusinessException(400,"财务明细编号无效");return new LinkedHashMap<>(store.one("SELECT DiagnosisDetailId,ListDiagnosisProjectId,CAST(TestPrice AS CHAR) AS TestPrice,CAST(OrginalDiscount AS CHAR) AS OrginalDiscount,PayStatus2,is_invoiced,IsCheckPass,IsSendMail,SendMailDt,PDFHttpUrl FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=?"+(lock?" FOR UPDATE":""),id));}
 private String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 static String payLabel(int exempt,int status){if(exempt==1)return "免除";return Map.of(0,"未结",1,"现金",2,"转账",3,"网银已支付",4,"忽略").getOrDefault(status,"");}
 private static final String FIELDS="is_invoiced,ListDiagnosisProjectId,InfoTestTypeId,InfoTestTypeModelId,MailTitle,CreateDt,IsChangeModelTemple,IsSendMail,TestName,TestNO,Price,DiscountReal,DiscountNext,UserNO,CityId,AnimalName,ChipNO,InfoAnimalId,SendReportDt,ReportNO,MasterName,InfoHospitalId,DeliverySendGerFee,RealFee,IsChangeMailModelTemple,ReceiveSampleDt,IsPayMoney,IsCheckPass,CtoG,GtoC,GenterNO,PDFHttpUrl,DiagnosisDetailId,TestPrice,OrginalPrice,PayStatus2,TestFee,CollectDt,SendMailDt,DetailIsActive";
 private static final Set<String> NUMERIC_TEXT=Set.of("Price","DiscountReal","DiscountNext","DeliverySendGerFee","RealFee","CtoG","GtoC","TestPrice","OrginalPrice","TestFee");
 @Transactional(readOnly=true) public Map<String,Object> list(Long hospital,String orderNo,int page,int size){
  read();if(page<0||size<1||size>100||hospital!=null&&(hospital<1||hospital>Integer.MAX_VALUE)||orderNo!=null&&orderNo.length()>250)throw new BusinessException(400,"财务查询无效");
  String from=" FROM vwdiagnosisdetail v LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=v.InfoHospitalId LEFT JOIN lin_lb_infoanimal a ON a.InfoAnimalId=v.InfoAnimalId WHERE v.InfoHospitalId<>0";var args=new ArrayList<Object>();if(hospital!=null){from+=" AND v.InfoHospitalId=?";args.add(hospital);}if(orderNo!=null&&!orderNo.isEmpty()){from+=" AND v.UserNO LIKE ?";args.add("%"+orderNo+"%");}
  var columns=new ArrayList<String>();for(String field:FIELDS.split(","))columns.add(NUMERIC_TEXT.contains(field)?"CAST(v."+field+" AS CHAR) AS "+field:"v."+field);
  var result=new LinkedHashMap<String,Object>(store.page("SELECT COUNT(*)"+from,"SELECT "+String.join(",",columns)+",h.HospitalName,a.AnimalName AS AnimalIdName"+from+" ORDER BY v.DiagnosisDetailId DESC",page,size,args.toArray()));
  @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)result.get("items");BigDecimal sum=BigDecimal.ZERO.setScale(2);
  for(var row:items){int exempt=row.get("IsCheckPass")==null?0:((Number)row.get("IsCheckPass")).intValue();row.put("PayStatus",row.get("PayStatus2")==null&&exempt!=1?"":payLabel(exempt,row.get("PayStatus2")==null?0:((Number)row.get("PayStatus2")).intValue()));row.put("HospitalName",Objects.toString(row.get("HospitalName"),""));row.put("AnimalIdName",Objects.toString(row.get("AnimalIdName"),""));if(exempt==0)sum=sum.add(new BigDecimal(Objects.toString(row.get("TestPrice"),"0"))).setScale(2,RoundingMode.DOWN);}
  result.put("pageSummary",Map.of("TestPrice",sum.toPlainString(),"label","本页合计（不含免除）"));return result;
 }
 @Transactional(readOnly=true) public Map<String,Object> detail(long id){read();var row=raw(id,false);row.put("editToken",token(row));return row;}
 @Transactional public long writeoff(long id,WriteoffCommand command){
  if(!policy(true)[2])throw new BusinessException(409,"单条核销尚未开启");
  if(command==null||command.type()==null||!Map.of("viewMoney",1,"viewTrance",2,"viewPass",4,"viewNoPay",0).containsKey(command.type())||command.requestKey()==null||!command.requestKey().matches("[A-Za-z0-9_-]{16,64}")||command.expectedToken()==null||!command.expectedToken().matches("[a-f0-9]{64}"))throw new BusinessException(400,"单条核销参数或版本无效");
  int status=Map.of("viewMoney",1,"viewTrance",2,"viewPass",4,"viewNoPay",0).get(command.type()),actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String canonical=store.encode(Map.of("id",id,"command",command));var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_FINANCE_WRITEOFF' AND request_id=? ORDER BY id DESC LIMIT 1",actor,command.requestKey());
  if(!prior.isEmpty())try{var json=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(json.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他核销操作");return id;}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"核销请求无法核对");}
  long order=((Number)raw(id,false).get("ListDiagnosisProjectId")).longValue();store.one("SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? FOR UPDATE",order);var before=raw(id,true);
  if(((Number)before.get("ListDiagnosisProjectId")).longValue()!=order||!token(before).equals(command.expectedToken()))throw new BusinessException(409,"明细已变更，请重新核对核销");
  // User-approved deviation: existing report/mail bodies remain untouched; create an empty copy only if absent.
  var body=store.jdbc().queryForList("SELECT diagnosisdetail_id FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=? FOR UPDATE",id);if(body.isEmpty())store.jdbc().update("INSERT INTO lin_lb_diagnosisdetail_info(diagnosisdetail_id,MailReport,ReportResult) VALUES(?,'','')",id);
  int sent=before.get("IsSendMail")==null?0:((Number)before.get("IsSendMail")).intValue(),exempt=before.get("IsCheckPass")==null?0:((Number)before.get("IsCheckPass")).intValue();
  var now=java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).withNano(0);var effect=LegacyDetailUpdateRules.effects(sent,Objects.toString(before.get("SendMailDt"),""),now);
  var project=store.one("SELECT AnimalName,MasterName,UserNO,ReportNO,DiagnosticResults FROM vwlistdiagnosisproject WHERE ListDiagnosisProjectId=?",order);
  String path="/Content/FilePdf/"+order+"_"+id+"/"+LegacyDetailUpdateRules.filename(project,effect,now);
  if(effect.completeParent())store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SendReportDt=?,SampleStatus=3 WHERE ListDiagnosisProjectId=?",now,order);
  Object discount=before.get("OrginalDiscount")==null?null:LegacyPricingRules.originalDiscount(new BigDecimal(before.get("OrginalDiscount").toString()));
  store.jdbc().update("UPDATE lin_lb_diagnosisdetail SET PayStatus2=?,OrginalDiscount=?,IsSendMail=?,IsCheckPass=?,SendMailDt=?,PDFHttpUrl=? WHERE DiagnosisDetailId=?",status,discount,sent,exempt,effect.sentAt(),path,id);
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_SINGLE_WRITEOFF','LEGACY_FINANCE_WRITEOFF',?,?,?,?)",actor,String.valueOf(id),command.requestKey(),store.encode(before),store.encode(Map.of("canonicalRequest",canonical,"parentCompleted",effect.completeParent(),"bodiesPreserved",true,"pdfPath",path)));return id;
 }
 @Transactional public int batch(BatchCommand c){
  if(!policy(true)[1])throw new BusinessException(409,"原财务批量维护尚未开启");if(c==null||c.targets()==null||c.targets().isEmpty()||c.targets().size()>500||c.field()==null||!Set.of("is_invoiced","PayStatus2").contains(c.field())||c.field().equals("is_invoiced")&&(c.value()!=0&&c.value()!=1)||c.field().equals("PayStatus2")&&!Set.of(0,1,2,4).contains(c.value()))throw new BusinessException(400,"财务批量操作无效");
  if(c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"财务请求号无效");var targets=new TreeMap<Long,String>();for(var target:c.targets()){if(target==null||target.editToken()==null||!target.editToken().matches("[a-f0-9]{64}")||targets.put(target.id(),target.editToken())!=null)throw new BusinessException(400,"财务明细重复或版本缺失");}
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);String canonical=store.encode(c);var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_FINANCE_BATCH' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty())try{var j=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(j.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他财务操作");return j.path("count").asInt();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"财务请求无法核对");}
  var before=new ArrayList<Map<String,Object>>();for(var target:targets.entrySet()){var row=raw(target.getKey(),true);if(!token(row).equals(target.getValue()))throw new BusinessException(409,"财务明细已变更，请刷新后核对整批操作");before.add(row);}
  // Source multi is a query UPDATE: no model callback, no balance transfer, no report/body changes.
  for(long id:targets.keySet())store.jdbc().update("UPDATE lin_lb_diagnosisdetail SET "+c.field()+"=? WHERE DiagnosisDetailId=?",c.value(),id);
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_FINANCE_FLAGS_UPDATED','LEGACY_FINANCE_BATCH',?,?,?,?)",actor,String.valueOf(targets.firstKey()),c.requestKey(),store.encode(before),store.encode(Map.of("count",targets.size(),"canonicalRequest",canonical)));return targets.size();
 }
}
