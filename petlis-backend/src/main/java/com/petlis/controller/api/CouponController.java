package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping({"coupons","admin/coupons"}) @RequiredArgsConstructor
public class CouponController {
 private final CouponService service;
 @GetMapping public Result<List<Map<String,Object>>> list(@RequestParam(defaultValue="all")String category){return Result.success(service.list(category));}
 @GetMapping("/templates") public Result<Map<String,Object>> templates(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.templates(page,size));}
 public record QuoteRequest(Integer laboratoryId,List<Integer> testTypeIds,Long userCouponId){}
 @PostMapping("/quote") public Result<CouponService.Quote> quote(@RequestBody QuoteRequest r){return Result.success(service.quote(r.laboratoryId(),r.testTypeIds(),r.userCouponId()));}
 @PostMapping public Result<Long> create(@RequestBody CouponService.Coupon r){return Result.success(service.create(r));}
 @PutMapping("/{id}") public Result<Void> update(@PathVariable long id,@RequestParam int totalQuantity,@RequestParam(required=false)String reason,@RequestBody CouponService.Coupon r){service.updateCoupon(id,r,totalQuantity,reason);return Result.success();}
 @DeleteMapping("/{id}") public Result<Void> deactivate(@PathVariable long id){service.deactivateCoupon(id);return Result.success();}
 @PostMapping("/{id}/users/{userId}") public Result<Long> grant(@PathVariable long id,@PathVariable int userId,@RequestBody(required=false)GrantRequest r){return Result.success(r==null?service.grant(id,userId):service.grant(id,userId,r.requestKey()));}
 public record GrantRequest(String requestKey){}
}
