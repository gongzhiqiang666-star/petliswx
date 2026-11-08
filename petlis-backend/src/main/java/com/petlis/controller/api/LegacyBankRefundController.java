package com.petlis.controller.api;
import com.petlis.common.response.Result;import com.petlis.service.impl.*;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("admin/legacy/bank-refunds") @RequiredArgsConstructor
public class LegacyBankRefundController {
 private final LegacyBankRefundService refunds;private final LegacyBankRefundDispatchService dispatch;
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(refunds.list(page,size));}
 @PostMapping public Result<Map<String,Object>> create(@RequestBody LegacyBankRefundService.Command command){return Result.success(dispatch.create(command));}
 @GetMapping("/{key}") public Result<Map<String,Object>> request(@PathVariable String key){return Result.success(refunds.request(key));}
 @PostMapping("/{key}/send") public Result<Map<String,Object>> send(@PathVariable String key){dispatch.execute(key,false);return Result.success(refunds.request(key));}
 @PostMapping("/{key}/query") public Result<Map<String,Object>> query(@PathVariable String key){dispatch.execute(key,true);return Result.success(refunds.request(key));}
 @PostMapping("/{key}/cancel") public Result<Map<String,Object>> cancel(@PathVariable String key){return Result.success(refunds.cancel(key));}
}
