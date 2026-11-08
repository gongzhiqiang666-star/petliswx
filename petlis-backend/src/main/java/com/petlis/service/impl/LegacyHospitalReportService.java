package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;import com.petlis.common.security.AccessService;import com.petlis.common.security.PlatformRole;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.http.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Authenticated hospital report access. PHP Pros link rule, no anonymous/old-link policy change. */
@Service @RequiredArgsConstructor
public class LegacyHospitalReportService {
 private final FeatureStore store;private final AccessService access;private final LegacyHospitalHistoryService history;private final PlatformFileService files;private final LegacyReportFileReader legacy;private final LegacyCustomerViewService views;
 @Transactional(readOnly=true) public ResponseEntity<?> download(long detail,int hospital){
  var actor=access.current();if(!(actor.isAdmin()||actor.hasRole(PlatformRole.DOCTOR)||actor.hasRole(PlatformRole.HOSPITAL_ADMIN))||hospital<1||!actor.canAccessHospital(hospital))throw new BusinessException(403,"无权查看该医院报告");
  if(!Boolean.TRUE.equals(history.capabilities().get("legacyHospitalReportDownload")))throw new BusinessException(409,"医院历史报告访问尚未开启");
  if(detail<1||detail>4294967295L)throw new BusinessException(400,"报告明细编号无效");
  var row=store.one("SELECT d.ListDiagnosisProjectId,d.IsSendMail,d.PDFHttpUrl,p.SampleStatus FROM lin_lb_diagnosisdetail d JOIN lin_lb_listdiagnosisproject p ON p.ListDiagnosisProjectId=d.ListDiagnosisProjectId WHERE d.DiagnosisDetailId=? AND p.InfoHospitalId=?",detail,hospital);
  if(((Number)row.get("SampleStatus")).intValue()<2||((Number)row.get("IsSendMail")).intValue()!=1)throw new BusinessException(409,"原报告查看条件尚未满足");long order=((Number)row.get("ListDiagnosisProjectId")).longValue();
  var permission=views.reportPermissionAt(detail,java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")));
  if(!Boolean.TRUE.equals(permission.get("reportAllowed")))throw new BusinessException(409,Boolean.TRUE.equals(permission.get("needPay"))?Objects.toString(permission.get("message"),"报告账单尚未结清"):"原报告文件引用尚未保存");
  boolean modern=Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_report_workflow WHERE detail_id=?)",Boolean.class,detail));
  if(modern){
   var published=store.jdbc().queryForList("SELECT w.id,w.pdf_file_id FROM lin_report_workflow w JOIN lin_file f ON f.id=w.pdf_file_id AND f.biz_type='REPORT' AND BINARY f.biz_id=BINARY CAST(w.id AS CHAR) AND f.hospital_id=w.hospital_id AND f.deleted_at IS NULL AND f.mime_type='application/pdf' WHERE w.detail_id=? AND w.order_id=? AND w.hospital_id=? AND w.status='PUBLISHED' ORDER BY w.published_at DESC,w.id DESC LIMIT 1",detail,order,hospital);
   if(published.isEmpty())throw new BusinessException(409,"新流程报告尚未发布或文件关联待核对");var file=published.get(0);var response=files.downloadReport(((Number)file.get("pdf_file_id")).longValue(),((Number)file.get("id")).longValue());var headers=new HttpHeaders();headers.putAll(response.getHeaders());privateHeaders(headers);return new ResponseEntity<>(response.getBody(),headers,response.getStatusCode());
  }
  String reference=Objects.toString(row.get("PDFHttpUrl"),"");if(reference.isBlank())throw new BusinessException(409,"原报告文件引用尚未保存");LegacyReportFileReader.Artifact artifact;try{artifact=legacy.read(reference);}catch(BusinessException error){if(error.getCode()==404)throw new BusinessException(404,"历史报告原件尚未取回，请联系实验室");if(error.getCode()==409&&"旧报告本机文件目录尚未配置".equals(error.getMessage()))throw new BusinessException(409,"历史报告原件存放尚未就绪，请联系实验室");throw error;}var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_PDF);headers.setContentDisposition(ContentDisposition.attachment().filename(artifact.filename(),StandardCharsets.UTF_8).build());headers.setContentLength(artifact.data().length);privateHeaders(headers);return ResponseEntity.ok().headers(headers).body(artifact.data());
 }
 private static void privateHeaders(HttpHeaders h){h.set(HttpHeaders.CACHE_CONTROL,"private, no-store");h.set("X-Content-Type-Options","nosniff");}
}
