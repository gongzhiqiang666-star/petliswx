package com.petlis.controller.api;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.response.Result;
import com.petlis.common.security.ReviewPermission;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController @RequiredArgsConstructor
public class QualificationController {
 private final ReviewPermission permission;
 @Value("${app.file.upload-path}") private String uploadPath;
 private final Map<String,java.util.ArrayDeque<Long>> attempts=new ConcurrentHashMap<>();
 @PostMapping("auth/qualifications") public Result<Map<String,String>> upload(@RequestParam("file")MultipartFile file,HttpServletRequest request)throws java.io.IOException {
  String ip=request.getRemoteAddr();long now=System.currentTimeMillis();var queue=attempts.computeIfAbsent(ip,k->new java.util.ArrayDeque<>());
  synchronized(queue){while(!queue.isEmpty() && queue.peekFirst()<now-3600000)queue.removeFirst();if(queue.size()>=20)throw new BusinessException(429,"上传过于频繁，请稍后再试");queue.addLast(now);}
  if(file.isEmpty() || file.getSize()>2*1024*1024)throw new BusinessException("资质文件需小于2MB");
  byte[] bytes=file.getBytes();String ext;
  if(bytes.length>=5 && new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))ext="pdf";
  else if(bytes.length>=8 && bytes[0]==(byte)0x89 && bytes[1]=='P' && bytes[2]=='N' && bytes[3]=='G')ext="png";
  else if(bytes.length>=3 && bytes[0]==(byte)0xff && bytes[1]==(byte)0xd8 && bytes[2]==(byte)0xff)ext="jpg";
  else throw new BusinessException("仅支持PDF、PNG或JPG文件");
  Path folder=Path.of(uploadPath,"qualifications").toAbsolutePath().normalize();Files.createDirectories(folder);
  String name=UUID.randomUUID().toString().replace("-","")+"."+ext;
  Files.write(folder.resolve(name),bytes,StandardOpenOption.CREATE_NEW);
  return Result.success(Map.of("url","/files/qualifications/"+name));
 }
 @GetMapping("files/qualifications/{name}") public ResponseEntity<FileSystemResource> read(@PathVariable String name){
  permission.requireReviewer();if(!name.matches("[a-f0-9]{32}\\.(pdf|png|jpg)"))throw new BusinessException(404,"文件不存在");Path file=Path.of(uploadPath,"qualifications",name).toAbsolutePath().normalize();if(!Files.isRegularFile(file))throw new BusinessException(404,"文件不存在");
  MediaType type=name.endsWith(".pdf")?MediaType.APPLICATION_PDF:name.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG;
  return ResponseEntity.ok().contentType(type).header("X-Content-Type-Options","nosniff").header("Cache-Control","no-store").body(new FileSystemResource(file));
 }
}
