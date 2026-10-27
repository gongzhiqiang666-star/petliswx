package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.MallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("mall") @RequiredArgsConstructor
public class MallController {
 private final MallService service;
 @GetMapping("/gifts") public Result<Map<String,Object>> gifts(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="false")boolean admin){return Result.success(admin?service.adminGifts(page,size):service.gifts(page,size));}
 @GetMapping("/gifts/{id}") public Result<Map<String,Object>> gift(@PathVariable long id){return Result.success(service.gift(id));}
 @PostMapping("/gifts") public Result<Long> create(@RequestBody MallService.Gift r){return Result.success(service.createGift(r));}
 @PostMapping("/redemptions") public Result<Long> redeem(@RequestBody MallService.Redemption r){return Result.success(service.redeem(r));}
 @GetMapping("/redemptions") public Result<Map<String,Object>> history(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="false")boolean admin){return Result.success(admin?service.adminRedemptions(page,size):service.history(page,size));}
 @PostMapping("/redemptions/{id}/cancel") public Result<Void> cancel(@PathVariable long id,@RequestBody CancelRequest r){service.cancel(id,r.reason());return Result.success();}
 public record CancelRequest(String reason){}
}
