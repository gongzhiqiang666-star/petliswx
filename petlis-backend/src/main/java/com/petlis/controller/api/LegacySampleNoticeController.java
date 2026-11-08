package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacySampleNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/sample-notices") @RequiredArgsConstructor
public class LegacySampleNoticeController {
 private final LegacySampleNoticeService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/orders/{id}/options") public Result<Map<String,Object>> options(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="100") int size){return Result.success(service.options(id,page,size));}
 @GetMapping("/orders/{id}/draft") public Result<Map<String,Object>> draft(@PathVariable long id,@RequestParam(defaultValue="1") long templateId){return Result.success(service.draft(id,templateId));}
 @GetMapping("/orders/{id}/messages") public Result<Map<String,Object>> messages(@PathVariable long id){return Result.success(service.messages(id));}
 @PostMapping("/orders/{id}") public Result<Long> queue(@PathVariable long id,@RequestBody LegacySampleNoticeService.Command command){return Result.success(service.queue(id,command));}
 @GetMapping("/messages/{id}") public Result<Map<String,Object>> message(@PathVariable long id){return Result.success(service.message(id));}
 @PostMapping("/messages/{id}/reconcile") public Result<Map<String,Object>> reconcile(@PathVariable long id,@RequestBody LegacySampleNoticeService.ReconcileCommand command){return Result.success(service.reconcile(id,command));}
 @PostMapping("/messages/{id}/cancel") public Result<Map<String,Object>> cancel(@PathVariable long id){return Result.success(service.cancel(id));}
 @PostMapping("/messages/{id}/dispatch") public Result<Map<String,Object>> dispatch(@PathVariable long id){return Result.success(service.dispatch(id));}
}
