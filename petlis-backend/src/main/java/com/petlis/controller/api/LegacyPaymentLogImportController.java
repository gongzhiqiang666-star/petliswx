package com.petlis.controller.api;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyPaymentLogImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("admin/legacy/payment-logs/import") @RequiredArgsConstructor
public class LegacyPaymentLogImportController {
 private final LegacyPaymentLogImportService service;
 @PostMapping(value="/preview",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Result<LegacyPaymentLogImportService.Preview> preview(@RequestParam MultipartFile file,@RequestParam(defaultValue="AUTO") String encoding,@RequestParam(defaultValue="name") String headerType)throws java.io.IOException{
  if(file.getSize()>2*1024*1024)throw new BusinessException(400,"请选择不超过2MB的导入文件");return Result.success(service.preview(file.getOriginalFilename(),file.getBytes(),encoding,headerType));
 }
 @PostMapping public Result<LegacyPaymentLogImportService.ImportResult> commit(@RequestBody LegacyPaymentLogImportService.ImportCommand command){return Result.success(service.commit(command));}
 @GetMapping("/template") public ResponseEntity<byte[]> template(){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=payment-log-template.csv").header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(service.template());}
}
