package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyReportMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/report-mails") @RequiredArgsConstructor
public class LegacyReportMailController {
 private final LegacyReportMailService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/details/{id}/options") public Result<Map<String,Object>> options(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="100") int size){return Result.success(service.options(id,page,size));}
 @GetMapping("/details/{id}/draft") public Result<Map<String,Object>> draft(@PathVariable long id,@RequestParam(defaultValue="0") long templateId){return Result.success(service.draft(id,templateId));}
 @GetMapping("/details/{id}/messages") public Result<Map<String,Object>> messages(@PathVariable long id){return Result.success(service.messages(id));}
 @GetMapping("/details/{id}/pdf") public ResponseEntity<byte[]> pdf(@PathVariable long id,@RequestParam(defaultValue="") String pdfTitle){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CACHE_CONTROL,"no-store").header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\"report.pdf\"").body(service.pdf(id,pdfTitle));}
 @PostMapping("/details/{id}") public Result<Long> queue(@PathVariable long id,@RequestBody LegacyReportMailService.Command command){return Result.success(service.queue(id,command));}
 @GetMapping("/messages/{id}") public Result<Map<String,Object>> message(@PathVariable long id){return Result.success(service.message(id));}
 @PostMapping("/messages/{id}/dispatch") public Result<Map<String,Object>> dispatch(@PathVariable long id){return Result.success(service.dispatch(id));}
 @PostMapping("/messages/{id}/cancel") public Result<Map<String,Object>> cancel(@PathVariable long id){return Result.success(service.cancel(id));}
 @PostMapping("/messages/{id}/reconcile") public Result<Map<String,Object>> reconcile(@PathVariable long id,@RequestBody LegacyReportMailService.ReconcileCommand command){return Result.success(service.reconcile(id,command));}
}
