package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** ADMIN customer-rule inspection; no new customer authentication policy is implied. */
@Service @RequiredArgsConstructor
public class LegacyCustomerReportService {
 private final FeatureStore store;private final LegacyCustomerViewService views;private final PlatformFileService files;private final LegacyReportFileReader legacy;
 @Transactional(readOnly=true) public ResponseEntity<?> download(LegacyCustomerViewService.Command command){
  if(!Boolean.TRUE.equals(views.capabilities().get("legacyCustomerReportDownload")))throw new BusinessException(409,"本机客户报告获取尚未开启");
  if(command==null||!"CID".equals(command.kind()))throw new BusinessException(400,"请选择报告明细");var preview=views.preview(command);
  if(!Boolean.TRUE.equals(preview.get("reportAllowed")))throw new BusinessException(409,"原报告付款或文件条件尚未满足");
  long detail=((Number)preview.get("detailId")).longValue();@SuppressWarnings("unchecked")var order=(Map<String,Object>)preview.get("project");long parent=((Number)order.get("ListDiagnosisProjectId")).longValue();long hospital=((Number)preview.get("hospitalId")).longValue();
  boolean modern=Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_report_workflow WHERE detail_id=?)",Boolean.class,detail));
  if(modern){
   var published=store.jdbc().queryForList("SELECT w.id,w.pdf_file_id FROM lin_report_workflow w JOIN lin_file f ON f.id=w.pdf_file_id AND f.biz_type='REPORT' AND BINARY f.biz_id=BINARY CAST(w.id AS CHAR) AND f.hospital_id=w.hospital_id AND f.deleted_at IS NULL AND f.mime_type='application/pdf' WHERE w.detail_id=? AND w.order_id=? AND w.hospital_id=? AND w.status='PUBLISHED' ORDER BY w.published_at DESC,w.id DESC LIMIT 1",detail,parent,hospital);
   if(published.isEmpty())throw new BusinessException(409,"新流程报告尚未发布或文件关联待核对");var row=published.get(0);var response=files.downloadReport(((Number)row.get("pdf_file_id")).longValue(),((Number)row.get("id")).longValue());var headers=new HttpHeaders();headers.putAll(response.getHeaders());privateHeaders(headers);return new ResponseEntity<>(response.getBody(),headers,response.getStatusCode());
  }
  var artifact=legacy.read(preview.get("reportReference").toString());var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_PDF);headers.setContentDisposition(ContentDisposition.attachment().filename(artifact.filename(),StandardCharsets.UTF_8).build());headers.setContentLength(artifact.data().length);privateHeaders(headers);return ResponseEntity.ok().headers(headers).body(artifact.data());
 }
 private static void privateHeaders(HttpHeaders headers){headers.set(HttpHeaders.CACHE_CONTROL,"private, no-store");headers.set("X-Content-Type-Options","nosniff");}
}
