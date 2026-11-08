package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Service;
import java.net.URI;import java.nio.channels.Channels;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Local bytes only: never follows HTTP or guesses a replacement report. */
@Service
public class LegacyReportFileReader {
 private final Path root;private final int maxBytes;
 public LegacyReportFileReader(@Value("${app.reports.legacy-storage-path:${APP_LEGACY_REPORTS_STORAGE_PATH:}}")String root,@Value("${app.reports.max-legacy-pdf-bytes:52428800}")long limit){this.root=root==null||root.isBlank()?null:Path.of(root).toAbsolutePath().normalize();if(limit<1||limit>Integer.MAX_VALUE-8)throw new BusinessException(409,"旧PDF大小限制配置无效");this.maxBytes=(int)limit;}
 public record Artifact(String filename,byte[] data){}
 public Artifact read(String reference){
  if(root==null)throw new BusinessException(409,"旧报告本机文件目录尚未配置");String relative=relative(reference);Path target=root.resolve(relative).normalize();
  if(!target.startsWith(root)||target.equals(root))throw new BusinessException(403,"报告路径超出允许范围");
  try{
   if(Files.isSymbolicLink(target))throw new BusinessException(403,"报告文件链接不允许访问");
   if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))throw new BusinessException(404,"原报告文件尚未取回本机");
   if(!target.toRealPath().startsWith(root.toRealPath()))throw new BusinessException(403,"报告路径超出允许范围");
   try(var channel=Files.newByteChannel(target,StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS);var input=Channels.newInputStream(channel)){
    long size=channel.size();if(size>maxBytes)throw new BusinessException(413,"报告超过当前读取容量，请调整本机文件限制");byte[] bytes=input.readNBytes(maxBytes+1);
    if(bytes.length>maxBytes)throw new BusinessException(413,"报告超过当前读取容量，请调整本机文件限制");if(bytes.length!=size)throw new BusinessException(409,"报告文件读取期间发生变化，请重试");
    String tail=new String(bytes,Math.max(0,bytes.length-4096),Math.min(4096,bytes.length),StandardCharsets.ISO_8859_1);
    if(bytes.length<5||!new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")||!tail.contains("%%EOF"))throw new BusinessException(422,"原报告文件不完整或不是PDF，请核对原件");
    return new Artifact(target.getFileName().toString(),bytes);
   }
  }catch(BusinessException e){throw e;}catch(java.io.IOException e){throw new BusinessException(500,"原报告本机文件读取失败");}
 }
 private static String relative(String reference){
  try{
   if(reference==null||reference.length()>4000)throw new IllegalArgumentException();var uri=URI.create(reference.replace('\\','/').replace(" ","%20"));boolean absolute=uri.isAbsolute();
   if(absolute&&(!Set.of("http","https").contains(uri.getScheme().toLowerCase(Locale.ROOT))||uri.getHost()==null||uri.getUserInfo()!=null))throw new IllegalArgumentException();
   String path=uri.getPath();if(path==null||path.isEmpty()||path.contains("\\")||path.contains(":"))throw new IllegalArgumentException();
   if(path.startsWith("/Content/FilePdf/"))path=path.substring("/Content/FilePdf/".length());else if(path.startsWith("Content/FilePdf/"))path=path.substring("Content/FilePdf/".length());else if(absolute||path.startsWith("/"))throw new IllegalArgumentException();
   if(!path.toLowerCase(Locale.ROOT).endsWith(".pdf")||path.chars().anyMatch(c->c<32||c==127))throw new IllegalArgumentException();for(String part:path.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))throw new IllegalArgumentException();return path;
  }catch(IllegalArgumentException e){throw new BusinessException(403,"原报告引用不是允许的本机PDF路径");}
 }
}
