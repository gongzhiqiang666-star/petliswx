package com.petlis.controller.api;
import com.petlis.common.response.Result;import com.petlis.service.impl.LegacyBankPaymentQuoteService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("admin/legacy/bank-payments") @RequiredArgsConstructor
public class LegacyBankPaymentController {
 private final LegacyBankPaymentQuoteService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/quote") public Result<Map<String,Object>> quote(@RequestParam String mode,@RequestParam(required=false) Long hospital,@RequestParam(required=false) String month,@RequestParam(required=false) String orderNo){return Result.success(service.quote(mode,hospital,month,orderNo));}
}
