package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("invoices") @RequiredArgsConstructor
public class InvoiceController {
 private final InvoiceService service;
 @GetMapping("/profiles") public Result<List<Map<String,Object>>> profiles(){return Result.success(service.profiles());}
 @PostMapping("/profiles") public Result<Long> add(@RequestBody InvoiceService.Profile r){return Result.success(service.addProfile(r));}
 @PutMapping("/profiles/{id}") public Result<Void> update(@PathVariable long id,@RequestBody InvoiceService.Profile r){service.updateProfile(id,r);return Result.success();}
 @GetMapping("/eligible-orders") public Result<Map<String,Object>> eligible(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.eligible(page,size));}
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.list(page,size));}
 @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable long id){return Result.success(service.detail(id));}
 @PostMapping public Result<Long> apply(@RequestBody InvoiceService.Application r){return Result.success(service.apply(r));}
 public record Decision(String invoiceUrl,String reason){}
 @PostMapping("/{id}/issue") public Result<Void> issue(@PathVariable long id,@RequestBody Decision r){service.issue(id,r.invoiceUrl());return Result.success();}
 @PostMapping("/{id}/reject") public Result<Void> reject(@PathVariable long id,@RequestBody Decision r){service.reject(id,r.reason());return Result.success();}
}
