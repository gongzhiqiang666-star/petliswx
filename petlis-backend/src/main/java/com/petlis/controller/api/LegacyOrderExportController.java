package com.petlis.controller.api;
import com.petlis.common.response.Result;import com.petlis.service.impl.LegacyOrderExportService;import lombok.RequiredArgsConstructor;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.nio.charset.StandardCharsets;import java.time.*;import java.time.format.DateTimeFormatter;import java.util.Map;
@RestController @RequestMapping("admin/legacy/order-export") @RequiredArgsConstructor public class LegacyOrderExportController {
 private final LegacyOrderExportService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping public ResponseEntity<byte[]> export(@RequestParam(required=false)Integer status){String name="数据导出"+LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+".xlsx";return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString()).header(HttpHeaders.CACHE_CONTROL,"private, no-store").header("X-Content-Type-Options","nosniff").body(service.export(status));}
}
