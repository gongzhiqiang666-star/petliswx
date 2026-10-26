package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.MallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("mall") @RequiredArgsConstructor
public class MallController {
 private final MallService service;
 @GetMapping("/gifts") public Result<Map<String,Object>> gifts(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.gifts(page,size));}
 @GetMapping("/gifts/{id}") public Result<Map<String,Object>> gift(@PathVariable long id){return Result.success(service.gift(id));}
 @PostMapping("/gifts") public Result<Long> create(@RequestBody MallService.Gift r){return Result.success(service.createGift(r));}
 @PostMapping("/redemptions") public Result<Long> redeem(@RequestBody MallService.Redemption r){return Result.success(service.redeem(r));}
 @GetMapping("/redemptions") public Result<Map<String,Object>> history(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.history(page,size));}
}
