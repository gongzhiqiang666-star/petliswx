package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LegacyDetailFormDefaultsTest {
 @Test void addFormKeepsOriginalTemplateIdsEmptyActualPriceAndTypeTwoMailRendering(){
  var service=mock(LegacyOrderDetailService.class, CALLS_REAL_METHODS);
  doReturn("邮件二型").when(service).renderDefaultBody(42, true);
  doReturn("报告二型").when(service).renderDefaultBody(42, false);
  doReturn(77L).when(service).defaultPriceId();
  doReturn("原标题").when(service).mailTitle(42,null);
  doNothing().when(service).requireFormRead(42);
  var row=service.formDefaults(42);
  assertEquals("",row.get("TestPrice"));assertEquals("10",row.get("OrginalDiscount"));
  assertEquals(0,row.get("InfoTestTypeId"));assertEquals(0,row.get("InfoTestTypeModelId"));
  assertEquals(1,row.get("DictMailDiagnosisReportId"));assertEquals(77L,row.get("infotesttype_price_id"));
  assertEquals("邮件二型",row.get("MailReport"));assertEquals("报告二型",row.get("ReportResult"));assertEquals("原标题",row.get("MailTitle"));
  assertTrue(row.get("CreateDt").toString().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
 }
}
