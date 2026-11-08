package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyPaymentLogImportFileTest {
 @Test void csvPreservesLeadingZerosQuotedMultilineAndFormulaText(){var f=LegacyPaymentLogImportFile.read("a.csv",("\ufeffTradeNoUser,Subject,TotalFee\r\n000123,\"中文,首行\n第二行\",-1.2345\r\n000124,=1+1,0\r\n").getBytes(StandardCharsets.UTF_8),"AUTO");assertEquals("UTF-8",f.encoding());assertEquals("000123",f.rows().get(0).cells().get(0));assertEquals("中文,首行\n第二行",f.rows().get(0).cells().get(1));assertEquals("=1+1",f.rows().get(1).cells().get(1));}
 @Test void explicitGbkAndBig5AndLegacyAutoDecodeWithoutReplacement(){for(String enc:new String[]{"GBK","Big5"}){var f=LegacyPaymentLogImportFile.read("a.csv","Subject,TotalFee\n醫院,1\n".getBytes(Charset.forName(enc)),enc);assertEquals("醫院",f.rows().get(0).cells().get(0));}var f=LegacyPaymentLogImportFile.read("a.csv","Subject\n中文医院\n".getBytes(Charset.forName("GBK")),"AUTO");assertEquals("GBK",f.encoding());assertThrows(BusinessException.class,()->LegacyPaymentLogImportFile.read("a.csv",new byte[]{(byte)0xff},"UTF-8"));}
 @Test void bothExcelFormatsReadFirstSheetRawNumbersAndUnevaluatedFormula()throws Exception{for(Workbook wb:new Workbook[]{new HSSFWorkbook(),new XSSFWorkbook()})try(wb){var sheet=wb.createSheet("first");var h=sheet.createRow(0);h.createCell(0).setCellValue("TradeNoUser");h.createCell(1).setCellValue("TotalFee");h.createCell(2).setCellValue("Subject");var r=sheet.createRow(1);r.createCell(0).setCellValue("000123");r.createCell(1).setCellValue(-12.3456);r.createCell(2).setCellFormula("1+1");wb.createSheet("ignored").createRow(0).createCell(0).setCellValue("not imported");var out=new ByteArrayOutputStream();wb.write(out);var f=LegacyPaymentLogImportFile.read(wb instanceof HSSFWorkbook?"a.xls":"a.xlsx",out.toByteArray(),"AUTO");assertEquals(1,f.rows().size());assertEquals("000123",f.rows().get(0).cells().get(0));assertEquals("-12.3456",f.rows().get(0).cells().get(1));assertEquals("=1+1",f.rows().get(0).cells().get(2));}}
 @Test void malformedUnsupportedOversizedAndExcessRowsReject(){assertThrows(BusinessException.class,()->LegacyPaymentLogImportFile.read("a.txt","a".getBytes(),"AUTO"));assertThrows(BusinessException.class,()->LegacyPaymentLogImportFile.read("a.xlsx","not a workbook".getBytes(),"AUTO"));assertThrows(BusinessException.class,()->LegacyPaymentLogImportFile.read("a.csv",new byte[2*1024*1024+1],"AUTO"));assertThrows(BusinessException.class,()->LegacyPaymentLogImportFile.read("a.csv",("Subject\n"+"a\n".repeat(1001)).getBytes(),"AUTO"));}
}
