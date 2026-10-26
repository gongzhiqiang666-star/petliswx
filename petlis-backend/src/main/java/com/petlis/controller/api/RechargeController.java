package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.dto.response.PaymentResponse;
import com.petlis.service.impl.RechargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("wallet/recharges") @RequiredArgsConstructor
public class RechargeController {
    private final RechargeService service;
    @PostMapping public Result<PaymentResponse> create(@RequestBody RechargeService.Request request){return Result.success(service.create(request));}
    @GetMapping public Result<Map<String,Object>> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.list(page,size));}
    @GetMapping("/{paymentNo}") public Result<Map<String,Object>> status(@PathVariable String paymentNo){return Result.success(service.status(paymentNo));}
    @PostMapping("/{paymentNo}/resume") public Result<PaymentResponse> resume(@PathVariable String paymentNo){return Result.success(service.resume(paymentNo));}
    @PostMapping("/{paymentNo}/close") public Result<Void> close(@PathVariable String paymentNo){service.close(paymentNo);return Result.success();}
}
