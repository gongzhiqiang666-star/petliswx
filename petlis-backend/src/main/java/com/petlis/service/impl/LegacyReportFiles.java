package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
/** GenterPdf names a file only. This guard never generates, replaces or publishes one. */
@Component
public class LegacyReportFiles {
 private final Path root;
 public LegacyReportFiles(@Value("${app.pdf.storage-path}") String root){this.root=Path.of(root).toAbsolutePath().normalize();}
 public static String filename(Map<String,Object> project,Map<String,Object> detail,String supplied,LocalDate today){
  String name=supplied;
  if(name==null||name.isEmpty()||name.equals("0")){
   Object value=detail.get("SendMailDt");String raw=value==null?null:value instanceof java.sql.Timestamp t?t.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")):value.toString().replace('T',' ');
   LocalDate day=raw==null?LocalDate.of(1970,1,1):raw.compareTo("1970-01-01")<=0?today:parseDay(raw);
   name=(name==null?"":name)+day.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))+","+text(project,"AnimalName")+","+text(project,"MasterName")+","+text(project,"UserNO")+","+text(project,"ReportNO")+text(project,"DiagnosticResults")+".pdf";
  }
  name=name.replace("\"","").replace("'","");
  if(!name.toLowerCase(Locale.ROOT).endsWith(".pdf")){int a=0,b=name.length();while(a<b&&name.charAt(a)==',')a++;while(b>a&&name.charAt(b-1)==',')b--;name=name.substring(a,b)+".pdf";}
  return name.replace(" ","").replace("&","和").replace("#","");
 }
 private static String text(Map<String,Object> row,String key){String s=Objects.toString(row.get(key),"");int a=0,b=s.length();while(a<b&&" \t\n\r\0\u000b".indexOf(s.charAt(a))>=0)a++;while(b>a&&" \t\n\r\0\u000b".indexOf(s.charAt(b-1))>=0)b--;return s.substring(a,b);}
 private static LocalDate parseDay(String value){try{return LocalDate.parse(value.substring(0,10));}catch(Exception e){throw new BusinessException(409,"原报告发送日期无法用于文件命名，请核对历史数据");}}
 public String path(long order,long detail,String filename){if(filename==null||filename.isBlank()||filename.contains("/")||filename.contains("\\")||filename.indexOf('\0')>=0||filename.getBytes(StandardCharsets.UTF_8).length>255)throw new BusinessException(400,"报告PDF文件名无效");return "/Content/FilePdf/"+order+"_"+detail+"/"+filename;}
 public Map<String,Object> inspect(String relative){
  try{
   if(relative==null||!relative.matches("/Content/FilePdf/[1-9][0-9]*_[1-9][0-9]*/[^/\\\\]+"))throw new BusinessException(400,"报告PDF路径无效");
   Path target=root.resolve(relative.substring("/Content/FilePdf/".length())).normalize();if(!target.startsWith(root))throw new BusinessException(400,"报告PDF路径无效");Path current=root.getRoot();for(Path part:root){current=current.resolve(part);if(Files.isSymbolicLink(current))throw new BusinessException(409,"报告目录不能是符号链接");}current=root;for(Path part:root.relativize(target)){current=current.resolve(part);if(Files.isSymbolicLink(current))throw new BusinessException(409,"报告文件不能是符号链接");}
   if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))throw new BusinessException(409,"请生成对应PDF报告后重试");long size=Files.size(target);if(size<5)throw new BusinessException(409,"报告PDF文件无效");var digest=MessageDigest.getInstance("SHA-256");
   try(var input=Files.newInputStream(target)){byte[] header=input.readNBytes(5);if(header.length<5||!new String(header,StandardCharsets.US_ASCII).equals("%PDF-"))throw new BusinessException(409,"报告文件不是PDF");digest.update(header);byte[] block=new byte[8192];int read;while((read=input.read(block))!=-1)digest.update(block,0,read);}
   return Map.of("path",relative,"size",size,"sha256",HexFormat.of().formatHex(digest.digest()));
  }catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(409,"报告PDF文件无法读取，请核对后重试");}
 }
 public byte[] read(String relative){inspect(relative);try{return Files.readAllBytes(root.resolve(relative.substring("/Content/FilePdf/".length())));}catch(Exception e){throw new BusinessException(409,"报告PDF文件无法读取");}}
}
