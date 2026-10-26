package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("review") @RequiredArgsConstructor
public class ReviewController {
 private final ReviewService service;
 @GetMapping("/registrations") public Result<Map<String,Object>> registrations(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.registrations(page,size));}
 @PostMapping("/registrations/{id}") public Result<Void> registration(@PathVariable int id,@RequestBody ReviewService.Decision r){service.registration(id,r);return Result.success();}
 @GetMapping("/profile-changes") public Result<Map<String,Object>> changes(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.profileChanges(page,size));}
 @PostMapping("/profile-changes/{id}") public Result<Void> change(@PathVariable long id,@RequestBody ReviewService.Decision r){service.profileChange(id,r);return Result.success();}
 @GetMapping("/orders") public Result<Map<String,Object>> orders(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.pendingOrders(page,size));}
 @PostMapping("/orders/{id}") public Result<Void> order(@PathVariable int id,@RequestBody ReviewService.Decision r){service.order(id,r);return Result.success();}
 @GetMapping("/invoices") public Result<Map<String,Object>> invoices(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.invoices(page,size));}
}
