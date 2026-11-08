package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyReportFilesTest {
 @Test void requiredButUnattachedReportDoesNotInheritSmtpAttachmentSizeLimit()throws Exception{
  var parent=java.nio.file.Path.of(System.getProperty("user.home"),".local/share/petlis/reports");java.nio.file.Files.createDirectories(parent);var root=java.nio.file.Files.createTempDirectory(parent,"it-report-size-");var folder=root.resolve("1_1");java.nio.file.Files.createDirectories(folder);var file=folder.resolve("synthetic.pdf");
  try{try(var out=new java.io.RandomAccessFile(file.toFile(),"rw")){out.write("%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII));out.setLength(21L*1024*1024);}var value=new LegacyReportFiles(root.toString()).inspect("/Content/FilePdf/1_1/synthetic.pdf");assertEquals(21L*1024*1024,((Number)value.get("size")).longValue());}
  finally{java.nio.file.Files.deleteIfExists(file);java.nio.file.Files.deleteIfExists(folder);java.nio.file.Files.deleteIfExists(root);}
 }
 @Test void originalFilenameKeepsLiteralThresholdAndCustomReplacementOrder(){
  var day=LocalDate.of(2030,6,7);Map<String,Object> p=Map.of("AnimalName"," 犬 & #甲 ","MasterName"," '客户' ","UserNO","NO","ReportNO","R","DiagnosticResults","诊断");
  assertEquals("2030.06.07,犬和甲,客户,NO,R诊断.pdf",LegacyReportFiles.filename(p,Map.of("SendMailDt",LocalDateTime.of(1900,1,1,0,0)),null,day));
  assertEquals("1970.01.01,犬和甲,客户,NO,R诊断.pdf",LegacyReportFiles.filename(p,Map.of(),null,day));
  assertTrue(LegacyReportFiles.filename(p,Map.of("SendMailDt","1970-01-01"),null,day).startsWith("2030.06.07"));
  assertTrue(LegacyReportFiles.filename(p,Map.of("SendMailDt",LocalDateTime.of(1970,1,1,0,0)),null,day).startsWith("1970.01.01"));
  assertTrue(LegacyReportFiles.filename(p,Map.of(),"0",day).startsWith("01970.01.01"));
  assertEquals("A和.pdf",LegacyReportFiles.filename(p,Map.of(),",,' A & # ',,",day));assertEquals("a.PDF",LegacyReportFiles.filename(p,Map.of(),"a.PDF",day));
 }
}
