package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.HospitalWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("account/hospital-wallets") @RequiredArgsConstructor
public class HospitalWalletController {
 private final HospitalWalletService service;
 @GetMapping public Result<List<Map<String,Object>>> balances(){return Result.success(service.balances());}
 @GetMapping("/{id}/transactions") public Result<Map<String,Object>> transactions(@PathVariable int id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="all") String category){return Result.success(service.transactions(id,page,size,category));}
}
