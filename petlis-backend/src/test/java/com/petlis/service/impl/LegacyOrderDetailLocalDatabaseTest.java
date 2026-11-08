package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyOrderDetailLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyOrderDetailService service;AccessService access;int actor,test;long order;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_detail_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'明细测试','normal')",prefix);order=LegacyDetailFixtures.order(store,prefix,actor);test=LegacyDetailFixtures.test(store,prefix);store.insert("INSERT INTO lin_lb_infotesttype_price(InfoTestTypeid,price,IsActive) VALUES(?,125.50,0)",test);store.insert("INSERT INTO lin_lb_infotesttype_price(InfoTestTypeid,price,IsActive) VALUES(?,200.00,1)",test);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyOrderDetailService(store,access,new LegacyReportLinks("https://petlis.invalid/","petlis-public-test-key"));gate(true,true);}
 void gate(boolean enabled,boolean write){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-order-details',?,'transaction-only detail test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",enabled,"writeEnabled",write)),actor);}
 String key(){return "detail_"+UUID.randomUUID().toString().replace("-","");}
 LegacyOrderDetailService.CreateCommand defaults(){return new LegacyOrderDetailService.CreateCommand(test,null,null,null,null,null,null,null,null,null,null,null,null,key());}
 void enableUpdates(){store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0",store.encode(Map.of("enabled",true,"writeEnabled",true,"updateEnabled",true)));}
 LegacyOrderDetailService.UpdateCommand edit(long id,String date,String request,String token){return new LegacyOrderDetailService.UpdateCommand(test,new BigDecimal("200"),"9",new BigDecimal("123.45"),"修改备注",0,"新标题",0,1,date,"<p>邮件更新</p>","<p>报告更新</p>",request,token);}
 @Test void updatePreservesBusinessFieldsAndCallbackSideEffectsWithReplayAndConflict(){
  long id=service.create(order,defaults());var before=service.detail(order,id);String token=before.get("editToken").toString();var command=edit(id,"2024-05-06",key(),token);
  assertEquals(409,assertThrows(BusinessException.class,()->service.update(order,id,command)).getCode());enableUpdates();
  assertEquals(id,service.update(order,id,command));var after=service.detail(order,id);
  assertEquals("123.45",after.get("TestPrice"));assertEquals("9.00",after.get("OrginalDiscount"));assertEquals("<p>报告更新</p>",after.get("ReportResult"));assertEquals("<p>邮件更新</p>",after.get("MailReport"));assertEquals(before.get("GenterNO"),after.get("GenterNO"));assertEquals(before.get("CreateDt"),after.get("CreateDt"));
  assertTrue(after.get("PDFHttpUrl").toString().startsWith("/Content/FilePdf/"+order+"_"+id+"/2024.05.06,"));
  assertEquals(3,store.jdbc().queryForObject("SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",Integer.class,order));
  assertEquals(id,service.update(order,id,command));assertEquals(409,assertThrows(BusinessException.class,()->service.update(order,id,edit(id,"",key(),token))).getCode());
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND action='LEGACY_ORDER_DETAIL_UPDATED'",Integer.class,actor));
 }
 @Test void emptyDateDoesNotCompleteParentAndOtherOrderCannotEditDetail(){
  long id=service.create(order,defaults());enableUpdates();var row=service.detail(order,id);
  int status=store.jdbc().queryForObject("SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",Integer.class,order);
  service.update(order,id,edit(id,"",key(),row.get("editToken").toString()));assertEquals(status,store.jdbc().queryForObject("SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",Integer.class,order));
  long other=LegacyDetailFixtures.order(store,prefix+"_other",actor);assertThrows(BusinessException.class,()->service.update(other,id,edit(id,"",key(),row.get("editToken").toString())));
 }
 @Test void addFormDefaultsUseOriginalFixedTemplatesWithoutWritingDetails(){
  long count=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail",Long.class);
  var row=service.formDefaults(order);
  for(var spec:List.of(new String[]{"MailReport","SELECT model_content AS body FROM lin_lb_dictmaildiagnosisreport WHERE id=1"},new String[]{"ReportResult","SELECT ModelInfo AS body FROM lin_lb_infotesttypemodel WHERE InfoTestTypeModelId=118"})){
   var bodies=store.jdbc().queryForList(spec[1]);Object body=bodies.isEmpty()?null:bodies.get(0).get("body");
   assertEquals(service.renderBody(order,0,body==null?null:body.toString(),2,"https://petlis.invalid/Project/View/Detail.aspx?CID="+LegacyLinkCodec.encrypt("0","petlis-public-test-key")),row.get(spec[0]));
  }
  assertEquals("",row.get("TestPrice"));assertEquals("10",row.get("OrginalDiscount"));assertEquals(0,row.get("InfoTestTypeModelId"));
  assertEquals(count,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail",Long.class));
  gate(false,false);assertEquals(409,assertThrows(BusinessException.class,()->service.formDefaults(order)).getCode());
 }
 @Test void modelDefaultsFirstPriceAndGlobalCountNumberArePreserved(){
  long count=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail",Long.class);long id=service.create(order,defaults());var row=service.detail(order,id);
  assertEquals(order+"-"+(count+1),row.get("GenterNO"));assertEquals("125.50",row.get("OrginalPrice"));assertEquals("125.50",row.get("TestPrice"));assertNull(row.get("OrginalDiscount"));
  for(String name:List.of("IsChangeModelTemple","IsChangeMailModelTemple","IsActive"))assertEquals(1,((Number)row.get(name)).intValue());
  for(String name:List.of("IsSendMail","IsCheckPass","PayStatus2","DiagnosisStatus","InfoTestTypeModelId"))assertEquals(0,((Number)row.get(name)).intValue());
  assertEquals("",row.get("PDFHttpUrl"));assertEquals("",row.get("DetailRemark"));assertEquals("",row.get("MailReport"));assertEquals("",row.get("ReportResult"));assertTrue(row.get("SendMailDt").toString().startsWith("1900-01-01"));
 }
 @Test void manualActualPriceIsNotRecalculatedAndBodyCopyIsExact(){
  String html=" <table style=\"width:100%\"><tr><td>中文 &amp; 原文</td></tr></table> ";var c=new LegacyOrderDetailService.CreateCommand(test,new BigDecimal("200.00"),"原价",new BigDecimal("123.456"),"备注",0,"标题",0,0,0,0,html,html,key());long id=service.create(order,c);var row=service.detail(order,id);assertEquals("200.00",row.get("OrginalPrice"));assertEquals("123.46",row.get("TestPrice"));assertEquals("10.00",row.get("OrginalDiscount"));assertEquals(html,row.get("MailReport"));assertEquals(html,row.get("ReportResult"));assertEquals("180.00",service.quote(order,new LegacyOrderDetailService.QuoteCommand("200","9")));
 }
 @Test void explicitZeroAndSentDateKeepOriginalMeaning(){var c=new LegacyOrderDetailService.CreateCommand(test,BigDecimal.ZERO,"0",BigDecimal.ZERO,"",null,null,1,1,null,null,null,null,key());long id=service.create(order,c);var row=service.detail(order,id);assertEquals("0.00",row.get("OrginalPrice"));assertEquals("0.00",row.get("TestPrice"));assertEquals("0.00",row.get("OrginalDiscount"));assertFalse(row.get("SendMailDt").toString().startsWith("1900"));assertEquals(1,((Number)row.get("IsCheckPass")).intValue());}
 @Test void noPriceAllowsExplicitManualPricesButCannotInventDefault(){store.jdbc().update("DELETE FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=?",test);assertThrows(BusinessException.class,()->service.create(order,defaults()));long id=service.create(order,new LegacyOrderDetailService.CreateCommand(test,BigDecimal.ONE,"10",BigDecimal.ONE,"",null,null,null,null,null,null,null,null,key()));assertEquals("1.00",service.detail(order,id).get("TestPrice"));}
 @Test void rabiesEmptyRuleIsPhpEmptyNotJavaBlank(){
  for(String remark:Arrays.asList(null,"","0"))assertThrows(BusinessException.class,()->service.create(order,new LegacyOrderDetailService.CreateCommand(1225,BigDecimal.ONE,"10",BigDecimal.ONE,remark,null,null,null,null,null,null,null,null,key())));
  long id=service.create(order,new LegacyOrderDetailService.CreateCommand(1225,BigDecimal.ONE,"10",BigDecimal.ONE," ",null,null,null,null,null,null,null,null,key()));assertEquals(" ",service.detail(order,id).get("DetailRemark"));
 }
 @Test void idempotentCreateDoesNotDuplicateBodyOrAuditAndGatesProtectMutation(){var c=defaults();long id=service.create(order,c);assertEquals(id,service.create(order,c));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=?",Integer.class,id));gate(true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.create(order,defaults())).getCode());gate(false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.detail(order,id)).getCode());}
 @SuppressWarnings("unchecked") @Test void formChoicesUseActiveTestsButKeepEveryPriceAndTemplateRelation(){
  var choices=service.tests(prefix,0,10);var rows=(List<Map<String,Object>>)choices.get("items");assertEquals(1,rows.size());assertEquals(test,((Number)rows.get(0).get("InfoTestTypeId")).intValue());
  store.jdbc().update("UPDATE lin_lb_infotesttype SET IsActive=0 WHERE InfoTestTypeId=?",test);assertEquals(0,((Number)service.tests(prefix,0,10).get("total")).intValue());
  assertEquals(2,((Number)service.testPrices(test,0,10).get("total")).intValue());
  store.insert("INSERT INTO lin_lb_infotesttypemodel(ModelName,ModelInfo,InfoTestTypeId,CreateDt) VALUES(?,?,?,NOW())",prefix+" \\u732b","<p>{宠物姓名}</p>",test);
  var templates=service.testTemplates(test,0,10);var template=((List<Map<String,Object>>)templates.get("items")).get(0);assertEquals(prefix+" 猫",template.get("ModelName"));assertFalse(template.containsKey("ModelInfo"));
  assertEquals(0,((Number)service.testTemplates(test+1,0,10).get("total")).intValue());
 }
 @Test void reportAndMailTemplateBodiesArePreservedAlongsideDecodedDisplay(){
  String html="<table style='border:1px solid'><tr><td>{宠物姓名} \\u732b</td></tr></table>";
  long model=store.insert("INSERT INTO lin_lb_infotesttypemodel(ModelName,ModelInfo,InfoTestTypeId,CreateDt) VALUES(?,?,?,NOW())",prefix,html,test);
  var report=service.reportTemplate(model);assertEquals(html,report.get("ModelInfo"));assertTrue(report.get("displayHtml").toString().contains("猫"));
  long mail=store.insert("INSERT INTO lin_lb_dictmaildiagnosisreport(model_type,model_content) VALUES(?,?)",prefix,html);
  assertEquals(html,service.mailTemplate(mail).get("model_content"));assertEquals(1,((Number)service.mailTemplates(prefix,0,10).get("total")).intValue());
  gate(false,false);assertEquals(409,assertThrows(BusinessException.class,()->service.reportTemplate(model)).getCode());
 }
 @Test void selectedTestTitleAppendsRawNameWithoutSeparatorOrActiveFilter(){
  String name=" 检测 & <项目> ";
  store.jdbc().update("UPDATE lin_lb_infotesttype SET TestName=?,IsActive=0 WHERE InfoTestTypeId=?",name,test);
  String base=service.mailTitle(order,null);
  assertEquals(base+name,service.testMailTitle(order,test));
  assertEquals(base,service.testMailTitle(order,0));
  gate(false,false);assertEquals(409,assertThrows(BusinessException.class,()->service.testMailTitle(order,test)).getCode());
 }
 @Test void defaultTitleUsesOriginalViewAndPreservesComponentWhitespace(){
  long animal=store.insert("INSERT INTO lin_lb_infoanimal(AnimalName,AnimalName_en,IsActive) VALUES('猫','Cat',1)");
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoAnimalId=?,AnimalType='短毛',AnimalName=' 糖 糖 ',MasterName=' 王 ' WHERE ListDiagnosisProjectId=?",animal,order);
  assertEquals("猫短毛 糖 糖 （ 王 ）（"+prefix+"）的检测报告",service.mailTitle(order,null));assertEquals("指定",service.mailTitle(order,",指定,,"));
  gate(false,false);assertEquals(409,assertThrows(BusinessException.class,()->service.mailTitle(order,null)).getCode());
 }
 @Test void renderingUsesWhitelistedOriginalBindingsAndExcludes246FromNameAggregation(){
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET AnimalName=?,ReceiveSampleDt='1900-01-01 00:00:00' WHERE ListDiagnosisProjectId=?",prefix,order);
  store.insert("INSERT INTO lin_lb_dictbindkey(BindTable,ColumnName,DescName) VALUES('ListDiagnosisProject','AnimalName',?)",prefix+"宠物");
  store.insert("INSERT INTO lin_lb_dictbindkey(BindTable,ColumnName,DescName) VALUES('ListDiagnosisProject','ReceiveSampleDt',?)",prefix+"收样");
  store.insert("INSERT INTO lin_lb_dictbindkey(BindTable,ColumnName,DescName) VALUES('ListDiagnosisProject','animalname',?)",prefix+"大小写");
  service.create(order,defaults());service.create(order,new LegacyOrderDetailService.CreateCommand(246,BigDecimal.ONE,"10",BigDecimal.ONE,"",null,null,null,null,null,null,null,null,key()));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM vwdiagnosisdetail WHERE ListDiagnosisProjectId=? AND InfoTestTypeId=246",Integer.class,order));
  String html=service.renderBody(order,0,"{"+prefix+"宠物}|{"+prefix+"收样}|{诊断项名称}|{"+prefix+"大小写}",2,"https://petlis.invalid/report");
  assertEquals(prefix+"|1900-01-01 00:00:00|"+prefix+"|{"+prefix+"大小写}",html);
  gate(false,false);assertEquals(409,assertThrows(BusinessException.class,()->service.renderBody(order,0,"正文",2,"https://petlis.invalid/report")).getCode());
 }
 @Test void reportAndMailRenderingUseServerConstructedLinksWithoutWritingTemplates(){
  long report=store.insert("INSERT INTO lin_lb_infotesttypemodel(ModelName,ModelInfo,InfoTestTypeId,CreateDt) VALUES(?,?,?,NOW())",prefix,"X{报告链接}|{诊断项名称}",test);
  long mail=store.insert("INSERT INTO lin_lb_dictmaildiagnosisreport(model_type,model_content) VALUES(?,?)",prefix,"X{用户付款链接}|{诊断项名称}");
  long detail=service.create(order,defaults());
  String rendered=service.renderReport(order,detail,report);assertTrue(rendered.contains("Project/View/Detail.aspx?CID="));assertTrue(rendered.contains(prefix));assertFalse(rendered.contains("{报告链接}"));
  String message=service.renderMail(order,detail,mail);assertTrue(message.contains("附件-报告地址:"));assertFalse(message.contains("{用户付款链接}"));
  assertEquals("X{报告链接}|{诊断项名称}",store.one("SELECT ModelInfo FROM lin_lb_infotesttypemodel WHERE InfoTestTypeModelId=?",report).get("ModelInfo"));
  assertEquals("",store.one("SELECT ReportResult FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=?",detail).get("ReportResult"));
 }
}
