package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.*;
/** Shared original beforeInsert/afterWrite effects; callers own authorization, locking and auditing. */
final class LegacyDetailModelWriter {
 private LegacyDetailModelWriter(){}
 static long insert(FeatureStore store,long orderId,LegacyOrderDetailService.CreateCommand c){
  String remark=c.detailRemark()==null?"":c.detailRemark();
  if(c.testTypeId()==1225&&(remark.isEmpty()||remark.equals("0")))throw new BusinessException(400,"备注不能为空,请填写备注(请检查是否因为选择了狂犬检测项,如果是请填写快递单号)");
  var prices=store.jdbc().queryForList("SELECT price FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=? LIMIT 1",c.testTypeId());
  BigDecimal fallback=prices.isEmpty()?null:new BigDecimal(prices.get(0).get("price").toString());
  BigDecimal original=c.originalPrice()==null?fallback:c.originalPrice(),actual=c.testPrice()==null?fallback:c.testPrice();
  if(original==null||actual==null)throw new BusinessException(400,"项目尚无价格记录，请填写原价和实价");
  var now=LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0);int sent=c.isSendMail()==null?0:c.isSendMail(),checked=c.isCheckPass()==null?0:c.isCheckPass();
  long count=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail",Long.class);
  Object discount=c.originalDiscount()==null?null:LegacyPricingRules.originalDiscount(c.originalDiscount());
  long id=store.insert("INSERT INTO lin_lb_diagnosisdetail(ListDiagnosisProjectId,GenterNO,InfoTestTypeId,InfoTestTypeModelId,MailTitle,CreateDt,IsChangeModelTemple,IsChangeMailModelTemple,IsSendMail,IsCheckPass,PDFHttpUrl,TestPrice,OrginalPrice,OrginalDiscount,SendMailDt,DetailRemark,PayStatus2,DiagnosisStatus,IsActive) VALUES(?,?,?,?,?,?,1,1,?,?,'',?,?,?,?,?,?,?,1)",orderId,orderId+"-"+(count+1),c.testTypeId(),c.modelId()==null?0:c.modelId(),c.mailTitle()==null?"":c.mailTitle(),now,sent,checked,actual,original,discount,sent==1?now:LocalDateTime.of(1900,1,1,0,0),remark,c.payStatus()==null?0:c.payStatus(),c.diagnosisStatus()==null?0:c.diagnosisStatus());
  store.jdbc().update("INSERT INTO lin_lb_diagnosisdetail_info(diagnosisdetail_id,MailReport,ReportResult) VALUES(?,?,?) ON DUPLICATE KEY UPDATE MailReport=VALUES(MailReport),ReportResult=VALUES(ReportResult)",id,c.mailReport()==null?"":c.mailReport(),c.reportResult()==null?"":c.reportResult());
  return id;
 }
}
