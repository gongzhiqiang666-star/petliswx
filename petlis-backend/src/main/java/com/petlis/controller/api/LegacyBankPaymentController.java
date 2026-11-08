package com.petlis.controller.api;
import com.petlis.common.response.Result;import com.petlis.service.impl.LegacyBankPaymentDispatchService;import com.petlis.service.impl.LegacyBankPaymentApplicationService;import com.petlis.service.impl.LegacyBankPaymentPreparationService;import com.petlis.service.impl.LegacyBankPaymentQuoteService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("admin/legacy/bank-payments") @RequiredArgsConstructor
public class LegacyBankPaymentController {
 private final LegacyBankPaymentQuoteService service;
 private final LegacyBankPaymentApplicationService application;
 private final LegacyBankPaymentDispatchService dispatch;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){var caps=new LinkedHashMap<>(application.capabilities());caps.putAll(dispatch.capabilities());return Result.success(caps);}
 @GetMapping("/quote") public Result<Map<String,Object>> quote(@RequestParam String mode,@RequestParam(required=false) Long hospital,@RequestParam(required=false) String month,@RequestParam(required=false) String orderNo){return Result.success(service.quote(mode,hospital,month,orderNo));}
 @PostMapping("/requests") public Result<LegacyBankPaymentPreparationService.Prepared> prepare(@RequestBody LegacyBankPaymentPreparationService.Command command){return Result.success(application.prepare(command));}
 @GetMapping("/requests") public Result<Map<String,Object>> requests(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(application.requests(page,size));}
 @GetMapping("/requests/{key}") public Result<Map<String,Object>> request(@PathVariable String key){return Result.success(application.request(key));}
 @PostMapping("/requests/{key}/send") public Result<Map<String,Object>> send(@PathVariable String key){dispatch.execute(key,false);return Result.success(application.request(key));}
 @PostMapping("/requests/{key}/query") public Result<Map<String,Object>> query(@PathVariable String key){dispatch.execute(key,true);return Result.success(application.request(key));}
}
