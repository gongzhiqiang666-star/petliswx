package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("coupons") @RequiredArgsConstructor
public class CouponController {
 private final CouponService service;
 @GetMapping public Result<List<Map<String,Object>>> list(@RequestParam(defaultValue="all")String category){return Result.success(service.list(category));}
 public record QuoteRequest(Integer laboratoryId,List<Integer> testTypeIds,Long userCouponId){}
 @PostMapping("/quote") public Result<CouponService.Quote> quote(@RequestBody QuoteRequest r){return Result.success(service.quote(r.laboratoryId(),r.testTypeIds(),r.userCouponId()));}
 @PostMapping public Result<Long> create(@RequestBody CouponService.Coupon r){return Result.success(service.create(r));}
 @PostMapping("/{id}/users/{userId}") public Result<Long> grant(@PathVariable long id,@PathVariable int userId){return Result.success(service.grant(id,userId));}
}
