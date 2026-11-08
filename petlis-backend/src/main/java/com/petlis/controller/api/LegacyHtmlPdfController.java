package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyHtmlPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/html-pdf") @RequiredArgsConstructor
public class LegacyHtmlPdfController {
 private final LegacyHtmlPdfService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @PostMapping("/preview") public ResponseEntity<byte[]> preview(@RequestBody LegacyHtmlPdfService.PreviewCommand command){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=report-preview.pdf").header(HttpHeaders.CACHE_CONTROL,"no-store").body(service.preview(command));}
}
