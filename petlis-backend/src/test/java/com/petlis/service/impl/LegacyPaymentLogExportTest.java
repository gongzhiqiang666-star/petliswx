package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.AccessService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyPaymentLogExportTest {
 @Test void completeQueryExportDoesNotIntroduceAnArbitraryTwentyThousandRowLimit()throws Exception{
  var jdbc=mock(JdbcTemplate.class);var rows=new ArrayList<Map<String,Object>>();for(int i=1;i<=20001;i++){var row=new LinkedHashMap<String,Object>();row.put("PayLogId",i);for(String f:LegacyPaymentLogService.FIELDS)row.put(f,"");row.put("TotalFee",new BigDecimal("0.00"));row.put("CreateDt",LocalDateTime.of(2020,2,29,13,14,15));rows.add(row);}
  doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"exportEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(rows).when(jdbc).queryForList(startsWith("SELECT PayLogId"),any(Object[].class));
  var service=new LegacyPaymentLogService(new FeatureStore(jdbc,new ObjectMapper()),mock(AccessService.class));byte[] bytes=service.export(Map.of());try(var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))){assertEquals(20001,workbook.getSheetAt(0).getLastRowNum());assertEquals("20001",workbook.getSheetAt(0).getRow(20001).getCell(0).getStringCellValue());}
 }
}
