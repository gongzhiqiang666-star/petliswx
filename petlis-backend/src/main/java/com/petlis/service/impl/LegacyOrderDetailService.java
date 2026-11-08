package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Legacy administrative details. Model callbacks are explicit transactional operations.
 * The original GenterPdf callback names a path; actual PDF delivery remains separately verified.
 */
@Service @RequiredArgsConstructor
public class LegacyOrderDetailService {
 private final FeatureStore store;private final AccessService access;private final LegacyReportLinks links;
 public record CreateCommand(int testTypeId,BigDecimal originalPrice,String originalDiscount,BigDecimal testPrice,String detailRemark,Integer modelId,String mailTitle,Integer isSendMail,Integer isCheckPass,Integer payStatus,Integer diagnosisStatus,String mailReport,String reportResult,String requestKey){}
 public record QuoteCommand(String price,String discount){}
 public record UpdateCommand(int testTypeId,BigDecimal originalPrice,String originalDiscount,BigDecimal testPrice,String detailRemark,Integer modelId,String mailTitle,Integer isSendMail,Integer isCheckPass,String sendMailDt,String mailReport,String reportResult,String requestKey,String expectedToken){}
 private record Policy(boolean enabled,boolean writable,boolean updatable){}
 private Policy policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));
  if(rows.size()!=1)return new Policy(false,false,false);
  try{var json=new ObjectMapper().readTree(String.valueOf(rows.get(0).get("config_value")));boolean enabled=json.path("enabled").isBoolean()&&json.path("enabled").asBoolean(),write=enabled&&json.path("writeEnabled").isBoolean()&&json.path("writeEnabled").asBoolean();return new Policy(enabled,write,write&&json.path("updateEnabled").isBoolean()&&json.path("updateEnabled").asBoolean());}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw error(409,"明细迁移参数无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("orderDetails",p.enabled(),"orderDetailWrites",p.writable(),"orderDetailUpdates",p.updatable());}
 private void read(){if(!policy(false).enabled())throw error(409,"原送检明细模块尚未开启");}
 private Map<String,Object> order(long id,boolean lock){if(id<1||id>2147483647L)throw error(400,"送检单编号无效");return store.one("SELECT ListDiagnosisProjectId,UserNO,SampleStatus,TestFee FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?"+(lock?" FOR UPDATE":""),id);}
 @Transactional(readOnly=true) public Map<String,Object> list(long orderId,int page,int size){
  read();order(orderId,false);if(page<0||size<1||size>100)throw error(400,"分页无效");
  return store.page("SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?","SELECT d.DiagnosisDetailId,d.GenterNO,d.InfoTestTypeId,t.TestName,t.TestNO,CAST(d.TestPrice AS CHAR) AS TestPrice,CAST(d.OrginalPrice AS CHAR) AS OrginalPrice,CAST(d.OrginalDiscount AS CHAR) AS OrginalDiscount,d.IsSendMail,d.IsCheckPass,d.PayStatus2,d.DiagnosisStatus,d.DetailRemark,d.CreateDt,d.SendMailDt FROM lin_lb_diagnosisdetail d LEFT JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=d.InfoTestTypeId WHERE d.ListDiagnosisProjectId=? ORDER BY d.DiagnosisDetailId DESC",page,size,orderId);
 }
 @Transactional(readOnly=true) public Map<String,Object> detail(long orderId,long id){read();order(orderId,false);var row=rawDetail(orderId,id,false);row.put("editToken",token(row));return row;}
 private LinkedHashMap<String,Object> rawDetail(long orderId,long id,boolean lock){var row=new LinkedHashMap<>(store.one("SELECT d.*,i.MailReport,i.ReportResult FROM lin_lb_diagnosisdetail d LEFT JOIN lin_lb_diagnosisdetail_info i ON i.diagnosisdetail_id=d.DiagnosisDetailId WHERE d.DiagnosisDetailId=? AND d.ListDiagnosisProjectId=?"+(lock?" FOR UPDATE":""),id,orderId));for(String field:List.of("OrginalPrice","OrginalDiscount","TestPrice"))if(row.get(field)!=null)row.put(field,new BigDecimal(row.get(field).toString()).toPlainString());return row;}
 private String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 @Transactional(readOnly=true) public String mailTitle(long orderId,String supplied){read();var project=store.one("SELECT InfoAnimalAnimalName,AnimalType,AnimalName,MasterName,UserNO FROM vwlistdiagnosisproject WHERE ListDiagnosisProjectId=?",orderId);return LegacyTemplateRules.mailTitle(project,supplied);}
 /** Base.mailtitle concatenates the raw selected TestName after the generated title, without trimming. */
 @Transactional(readOnly=true) public String testMailTitle(long orderId,long testId){
  String title=mailTitle(orderId,null);var tests=store.jdbc().queryForList("SELECT TestName FROM lin_lb_infotesttype WHERE InfoTestTypeId=?",testId);
  Object name=tests.isEmpty()?null:tests.get(0).get("TestName");return title+(name==null?"":name.toString());
 }
 /** Original add GET defaults differ from save-model defaults and from explicit mail-template loading. */
 @Transactional(readOnly=true) public Map<String,Object> formDefaults(long orderId){
  requireFormRead(orderId);var row=new LinkedHashMap<String,Object>();
  row.put("MailReport",renderDefaultBody(orderId,true));row.put("ReportResult",renderDefaultBody(orderId,false));
  row.put("CreateDt",LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  row.put("InfoTestTypeId",0);row.put("infotesttype_price_id",defaultPriceId());row.put("OrginalDiscount","10");row.put("TestPrice","");
  row.put("DictMailDiagnosisReportId",1);row.put("InfoTestTypeModelId",0);row.put("MailTitle",mailTitle(orderId,null));return row;
 }
 void requireFormRead(long orderId){read();order(orderId,false);}
 Long defaultPriceId(){var prices=store.jdbc().queryForList("SELECT IID FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=1 LIMIT 1");return prices.isEmpty()?null:((Number)prices.get(0).get("IID")).longValue();}
 String renderDefaultBody(long orderId,boolean mail){
  var templates=store.jdbc().queryForList(mail?"SELECT model_content AS body FROM lin_lb_dictmaildiagnosisreport WHERE id=1":"SELECT ModelInfo AS body FROM lin_lb_infotesttypemodel WHERE InfoTestTypeModelId=118");
  Object body=templates.isEmpty()?null:templates.get(0).get("body");
  return renderBody(orderId,0,body==null?null:body.toString(),2,links.detail(0));
 }
 @Transactional(readOnly=true) public String quote(long orderId,QuoteCommand command){read();order(orderId,false);if(command==null)throw error(400,"价格或折扣格式无效");return LegacyPricingRules.quote(command.price(),command.discount());}
 /** Internal composition entry: link must be constructed by the server link provider, never taken from an HTTP field. */
 @Transactional(readOnly=true) public String renderBody(long orderId,long detailId,String body,int type,String serverLink){
  read();return renderBodyCore(orderId,detailId,body,type,serverLink);
 }
 @Transactional(readOnly=true) public String renderSampleNoticeBody(long orderId,String body){
  access.requireAdmin();String link=LegacyDictionarySelector.display(body).indexOf("{报告链接}")>0?links.detail(0):"";return renderBodyCore(orderId,0,body,2,link);
 }
 private String renderBodyCore(long orderId,long detailId,String body,int type,String serverLink){
  order(orderId,false);if(detailId<0)throw error(400,"明细编号无效");
  if(detailId>0)store.one("SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=? AND ListDiagnosisProjectId=?",detailId,orderId);
  var project=new LinkedHashMap<>(store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",orderId));
  var rows=new HashMap<String,Map<String,Object>>();rows.put("ListDiagnosisProject",project);
  String[][] related={{"InfoHospital","lin_lb_infohospital","InfoHospitalId"},{"DictSex","lin_lb_dictsex","DictSexId"},{"InfoAnimal","lin_lb_infoanimal","InfoAnimalId"},{"InfoSampleType","lin_lb_infosampletype","InfoSampleTypeId"}};
  for(var relation:related){var found=store.jdbc().queryForList("SELECT * FROM "+relation[1]+" WHERE "+relation[2]+"=?",project.get(relation[2]));rows.put(relation[0],found.isEmpty()?Map.of():new LinkedHashMap<>(found.get(0)));}
  var details=store.jdbc().queryForList("SELECT * FROM vwdiagnosisdetail WHERE DiagnosisDetailId=?",detailId);rows.put("VwDiagnosisDetail",details.isEmpty()?Map.of():new LinkedHashMap<>(details.get(0)));
  var bindings=new ArrayList<LegacyTemplateRules.Binding>();
  for(String table:List.of("ListDiagnosisProject","InfoHospital","DictSex","InfoAnimal","InfoSampleType","VwDiagnosisDetail"))for(var b:store.jdbc().queryForList("SELECT ColumnName,DescName FROM lin_lb_dictbindkey WHERE BindTable=?",table))bindings.add(new LegacyTemplateRules.Binding(table,b.get("ColumnName").toString(),b.get("DescName").toString()));
  String names=store.jdbc().queryForObject("SELECT GROUP_CONCAT(TestName) FROM vwdiagnosisdetail WHERE ListDiagnosisProjectId=? AND InfoTestTypeId<>246",String.class,orderId);
  return LegacyTemplateRules.render(body,bindings,rows,serverLink,LocalDate.now(ZoneId.of("Asia/Shanghai")),names,type);
 }
 @Transactional(readOnly=true) public String renderReport(long orderId,long detailId,long templateId){read();var template=reportTemplate(templateId);String body=template.get("ModelInfo")==null?null:template.get("ModelInfo").toString();return renderBody(orderId,detailId,body,2,links.detail(detailId));}
 @Transactional(readOnly=true) public String renderMail(long orderId,long detailId,long templateId){read();var template=mailTemplate(templateId);String body=template.get("model_content")==null?null:template.get("model_content").toString();return renderBody(orderId,detailId,body,1,links.detail(detailId));}
 private static void page(int page,int size){if(page<0||size<1||size>100)throw error(400,"分页无效");}
 private static String search(String q){if(q!=null&&q.length()>1000)throw error(400,"搜索内容过长");return "%"+(q==null?"":q.trim())+"%";}
 @Transactional(readOnly=true) public Map<String,Object> tests(String q,int page,int size){read();page(page,size);String term=search(q);return store.page("SELECT COUNT(*) FROM lin_lb_infotesttype WHERE IsActive=1 AND (TestName LIKE ? OR TestNO LIKE ?)","SELECT InfoTestTypeId,TestName,TestNO FROM lin_lb_infotesttype WHERE IsActive=1 AND (TestName LIKE ? OR TestNO LIKE ?) ORDER BY InfoTestTypeId",page,size,term,term);}
 @Transactional(readOnly=true) public Map<String,Object> testPrices(long testId,int page,int size){read();page(page,size);return store.page("SELECT COUNT(*) FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=?","SELECT IID,InfoTestTypeid,CAST(price AS CHAR) AS price,IsActive FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=? ORDER BY IID",page,size,testId);}
 @Transactional(readOnly=true) public Map<String,Object> testTemplates(long testId,int page,int size){
  read();page(page,size);var result=store.page("SELECT COUNT(*) FROM lin_lb_infotesttypemodel WHERE InfoTestTypeId=?","SELECT InfoTestTypeModelId,ModelName,CreateDt FROM lin_lb_infotesttypemodel WHERE InfoTestTypeId=? ORDER BY InfoTestTypeModelId",page,size,testId);
  @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)result.get("items");
  for(var row:items){String name=LegacyDictionarySelector.display(row.get("ModelName"));row.put("ModelName",name);row.put("ModelName1",name);}return result;
 }
 @Transactional(readOnly=true) public Map<String,Object> reportTemplate(long id){read();var row=new LinkedHashMap<>(store.one("SELECT InfoTestTypeModelId,ModelName,ModelInfo,InfoTestTypeId,CreateDt,updatetime FROM lin_lb_infotesttypemodel WHERE InfoTestTypeModelId=?",id));row.put("displayName",LegacyDictionarySelector.display(row.get("ModelName")));row.put("displayHtml",LegacyDictionarySelector.display(row.get("ModelInfo")));return row;}
 @Transactional(readOnly=true) public Map<String,Object> mailTemplates(String q,int page,int size){read();page(page,size);return store.page("SELECT COUNT(*) FROM lin_lb_dictmaildiagnosisreport WHERE model_type LIKE ?","SELECT id,model_type,createtime,updatetime FROM lin_lb_dictmaildiagnosisreport WHERE model_type LIKE ? ORDER BY id",page,size,search(q));}
 @Transactional(readOnly=true) public Map<String,Object> mailTemplate(long id){read();return store.one("SELECT id,model_type,model_content,createtime,updatetime FROM lin_lb_dictmaildiagnosisreport WHERE id=?",id);}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public long create(long orderId,CreateCommand c){
  // Serializes new-service insertions to preserve the original global COUNT+1 number without DDL.
  if(!policy(true).writable())throw error(409,"原送检明细维护尚未开启");
  if(c==null||c.testTypeId()<1)throw error(400,"请选择检测项目");key(c.requestKey());
  text(c.detailRemark(),100,"备注");text(c.mailTitle(),250,"邮件标题");if(c.modelId()!=null&&c.modelId()<0)throw error(400,"模板编号无效");flag(c.isSendMail());flag(c.isCheckPass());money(c.originalPrice());money(c.testPrice());
  String remark=c.detailRemark()==null?"":c.detailRemark();
  if(c.testTypeId()==1225&&(remark.isEmpty()||remark.equals("0")))throw error(400,"备注不能为空,请填写备注(请检查是否因为选择了狂犬检测项,如果是请填写快递单号)");
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  var payload=new TreeMap<String,Object>();payload.put("orderId",orderId);payload.put("command",c);String canonical=store.encode(payload);
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_ORDER_DETAIL' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty()){
   try{var json=new ObjectMapper().readTree(String.valueOf(prior.get(0).get("after_json")));if(!canonical.equals(json.path("canonicalRequest").asText())||!json.path("detailId").canConvertToLong())throw error(409,"请求号已用于其他明细操作");return json.path("detailId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw error(409,"原明细请求无法校验");}
  }
  order(orderId,true);
  var prices=store.jdbc().queryForList("SELECT price FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=? LIMIT 1",c.testTypeId());
  BigDecimal fallback=prices.isEmpty()?null:new BigDecimal(prices.get(0).get("price").toString());
  BigDecimal original=c.originalPrice()==null?fallback:c.originalPrice(),actual=c.testPrice()==null?fallback:c.testPrice();
  if(original==null||actual==null)throw error(400,"项目尚无价格记录，请填写原价和实价");
  var now=LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0);int sent=c.isSendMail()==null?0:c.isSendMail(),checked=c.isCheckPass()==null?0:c.isCheckPass();
  long count=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail",Long.class);
  Object discount=c.originalDiscount()==null?null:LegacyPricingRules.originalDiscount(c.originalDiscount());
  long id=store.insert("INSERT INTO lin_lb_diagnosisdetail(ListDiagnosisProjectId,GenterNO,InfoTestTypeId,InfoTestTypeModelId,MailTitle,CreateDt,IsChangeModelTemple,IsChangeMailModelTemple,IsSendMail,IsCheckPass,PDFHttpUrl,TestPrice,OrginalPrice,OrginalDiscount,SendMailDt,DetailRemark,PayStatus2,DiagnosisStatus,IsActive) VALUES(?,?,?,?,?,?,1,1,?,?,'',?,?,?,?,?,?,?,1)",orderId,orderId+"-"+(count+1),c.testTypeId(),c.modelId()==null?0:c.modelId(),c.mailTitle()==null?"":c.mailTitle(),now,sent,checked,actual,original,discount,sent==1?now:LocalDateTime.of(1900,1,1,0,0),remark,c.payStatus()==null?0:c.payStatus(),c.diagnosisStatus()==null?0:c.diagnosisStatus());
  store.jdbc().update("INSERT INTO lin_lb_diagnosisdetail_info(diagnosisdetail_id,MailReport,ReportResult) VALUES(?,?,?) ON DUPLICATE KEY UPDATE MailReport=VALUES(MailReport),ReportResult=VALUES(ReportResult)",id,c.mailReport()==null?"":c.mailReport(),c.reportResult()==null?"":c.reportResult());
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,'LEGACY_ORDER_DETAIL_CREATED','LEGACY_ORDER_DETAIL',?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(Map.of("detailId",id,"orderId",orderId,"canonicalRequest",canonical)));
  return id;
 }
 private static void text(String value,int max,String label){if(value!=null&&value.codePointCount(0,value.length())>max)throw error(400,label+"超过原字段长度");}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public long update(long orderId,long id,UpdateCommand c){
  if(!policy(true).updatable())throw error(409,"原检测明细修改尚未开启");
  if(c==null||c.testTypeId()<1)throw error(400,"请选择检测项目");key(c.requestKey());
  if(c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw error(400,"请重新打开明细后编辑");
  flag(c.isSendMail());flag(c.isCheckPass());money(c.originalPrice());money(c.testPrice());text(c.detailRemark(),100,"备注");text(c.mailTitle(),250,"邮件标题");
  if(c.modelId()!=null&&c.modelId()<0)throw error(400,"模板编号无效");
  if(c.originalPrice()==null||c.testPrice()==null)throw error(400,"请填写原价和实价");
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  var payload=new TreeMap<String,Object>();payload.put("operation","UPDATE");payload.put("orderId",orderId);payload.put("id",id);payload.put("command",c);String canonical=store.encode(payload);
  var previous=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_ORDER_DETAIL' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!previous.isEmpty()){
   try{var value=new ObjectMapper().readTree(String.valueOf(previous.get(0).get("after_json")));if(!canonical.equals(value.path("canonicalRequest").asText())||value.path("detailId").asLong()!=id)throw error(409,"请求号已用于其他明细操作");return id;}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw error(409,"原明细请求无法校验");}
  }
  order(orderId,true);var before=rawDetail(orderId,id,true);if(!token(before).equals(c.expectedToken()))throw error(409,"明细已被其他操作修改，请重新打开核对后保存");
  var now=LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0);int sent=c.isSendMail()==null?0:c.isSendMail(),checked=c.isCheckPass()==null?0:c.isCheckPass();
  var effect=LegacyDetailUpdateRules.effects(sent,c.sendMailDt(),now);
  var project=store.one("SELECT AnimalName,MasterName,UserNO,ReportNO,DiagnosticResults FROM vwlistdiagnosisproject WHERE ListDiagnosisProjectId=?",orderId);
  String path="/Content/FilePdf/"+orderId+"_"+id+"/"+LegacyDetailUpdateRules.filename(project,effect,now);
  // Preserve PHP's condition exactly: non-empty submitted date, not only IsSendMail=1.
  if(effect.completeParent())store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SendReportDt=?,SampleStatus=3 WHERE ListDiagnosisProjectId=?",now,orderId);
  store.jdbc().update("UPDATE lin_lb_diagnosisdetail SET InfoTestTypeId=?,InfoTestTypeModelId=?,OrginalPrice=?,OrginalDiscount=?,TestPrice=?,DetailRemark=?,MailTitle=?,IsSendMail=?,IsCheckPass=?,SendMailDt=?,PDFHttpUrl=? WHERE DiagnosisDetailId=? AND ListDiagnosisProjectId=?",c.testTypeId(),c.modelId()==null?0:c.modelId(),c.originalPrice(),LegacyPricingRules.originalDiscount(c.originalDiscount()),c.testPrice(),c.detailRemark()==null?"":c.detailRemark(),c.mailTitle()==null?"":c.mailTitle(),sent,checked,effect.sentAt(),path,id,orderId);
  store.jdbc().update("INSERT INTO lin_lb_diagnosisdetail_info(diagnosisdetail_id,MailReport,ReportResult) VALUES(?,?,?) ON DUPLICATE KEY UPDATE MailReport=VALUES(MailReport),ReportResult=VALUES(ReportResult)",id,c.mailReport()==null?"":c.mailReport(),c.reportResult()==null?"":c.reportResult());
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_ORDER_DETAIL_UPDATED','LEGACY_ORDER_DETAIL',?,?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(before),store.encode(Map.of("detailId",id,"orderId",orderId,"canonicalRequest",canonical,"parentCompleted",effect.completeParent(),"pdfPath",path)));
  return id;
 }
 private static void money(BigDecimal value){if(value!=null&&(value.precision()>128||value.scale()>128||value.abs().compareTo(new BigDecimal("9999999999999999.99"))>0))throw error(400,"金额超出原字段范围");}
 private static void flag(Integer value){if(value!=null&&value!=0&&value!=1)throw error(400,"明细标志无效");}
 private static void key(String value){if(value==null||!value.matches("[A-Za-z0-9_-]{16,64}"))throw error(400,"明细请求号无效");}
 private static BusinessException error(int code,String text){return new BusinessException(code,text);}
}
