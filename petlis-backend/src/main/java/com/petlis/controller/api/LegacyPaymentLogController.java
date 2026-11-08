package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyPaymentLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/payment-logs") @RequiredArgsConstructor
public class LegacyPaymentLogController {
 private final LegacyPaymentLogService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.list(q,page,size));}
 @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable long id){return Result.success(service.detail(id));}
 @PostMapping("/batch") public Result<LegacyPaymentLogService.BatchResult> batch(@RequestBody LegacyPaymentLogService.BatchCommand command){return Result.success(service.batch(command));}
 @PostMapping public Result<Long> create(@RequestBody LegacyPaymentLogService.Command command){return Result.success(service.save(null,command));}
 @PutMapping("/{id}") public Result<Long> update(@PathVariable long id,@RequestBody LegacyPaymentLogService.Command command){return Result.success(service.save(id,command));}
 @PostMapping("/{id}/delete") public Result<Long> delete(@PathVariable long id,@RequestBody LegacyPaymentLogService.DeleteCommand command){return Result.success(service.delete(id,command));}
}
