package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
@Component
public class LegacyHtmlPdfRenderer {
 private final String node,script,chrome;private final Path storage;private final Semaphore slots=new Semaphore(2);
 public LegacyHtmlPdfRenderer(@Value("${app.reports.html-pdf-node:node}")String node,@Value("${app.reports.html-pdf-script:}")String script,@Value("${app.reports.html-pdf-browser:}")String chrome,@Value("${app.pdf.storage-path}")String storage){this.node=node;this.script=script;this.chrome=chrome;this.storage=Path.of(storage);}
 public byte[] render(String html){
  if(html==null||html.isBlank())throw new BusinessException(400,"PDF正文不能为空");if(html.getBytes(StandardCharsets.UTF_8).length>2*1024*1024)throw new BusinessException(400,"PDF正文超过当前预览容量，请拆分或配置后处理");
  if(script.isBlank()||chrome.isBlank()||!Files.isRegularFile(Path.of(script))||!Files.isRegularFile(Path.of(chrome)))throw new BusinessException(409,"HTML报告PDF引擎尚未配置");
  if(!slots.tryAcquire())throw new BusinessException(409,"PDF引擎繁忙，请稍后重试");Path directory=null;Process process=null;
  try{
   Files.createDirectories(storage);directory=Files.createTempDirectory(storage,".html-preview-");Path output=directory.resolve("preview.pdf"),log=directory.resolve("renderer.log");
   process=new ProcessBuilder(node,script,output.toString(),chrome).redirectErrorStream(true).redirectOutput(log.toFile()).start();
   try(var input=process.getOutputStream()){input.write(html.getBytes(StandardCharsets.UTF_8));}
   if(!process.waitFor(50,TimeUnit.SECONDS))throw new BusinessException(504,"PDF生成超时，请核对正文后重试");
   if(process.exitValue()!=0){String error=Files.readString(log);if(error.equals("MISSING_ASSETS"))throw new BusinessException(422,"正文图片或外部资源不可用，已停止生成，请先补齐本地素材");if(error.equals("UNSUPPORTED_CONTENT"))throw new BusinessException(422,"正文包含当前PDF引擎不支持的嵌入内容，请先核对");throw new BusinessException(500,"HTML报告PDF生成失败，请检查引擎配置");}
   if(!Files.isRegularFile(output)||Files.size(output)>20*1024*1024)throw new BusinessException(500,"PDF输出无效或超过预览容量");byte[] bytes=Files.readAllBytes(output);if(bytes.length<5||!new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))throw new BusinessException(500,"PDF输出无效");return bytes;
  }catch(InterruptedException e){Thread.currentThread().interrupt();throw new BusinessException(503,"PDF生成已中断");}catch(java.io.IOException e){throw new BusinessException(500,"HTML报告PDF引擎无法执行");}
  finally{if(process!=null&&process.isAlive()){process.descendants().forEach(p->p.destroyForcibly());process.destroyForcibly();}if(directory!=null)try{Files.deleteIfExists(directory.resolve("preview.pdf"));Files.deleteIfExists(directory.resolve("renderer.log"));Files.deleteIfExists(directory);}catch(java.io.IOException ignored){}slots.release();}
 }
}
