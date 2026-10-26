package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService service;
    @GetMapping public Result<Map<String,Object>> summary(){return Result.success(service.summary());}
    @GetMapping("/wallet/transactions") public Result<Map<String,Object>> wallet(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="all") String category){return Result.success(service.transactions(false,page,size,category));}
    @GetMapping("/points/transactions") public Result<Map<String,Object>> points(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="all") String category){return Result.success(service.transactions(true,page,size,category));}
    @GetMapping(value="/transactions/export",produces="text/csv;charset=UTF-8") public ResponseEntity<String> export(@RequestParam(defaultValue="false") boolean points,@RequestParam(defaultValue="all") String category){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=transactions.csv").body(service.exportTransactions(points,category));}
    @GetMapping("/preferences") public Result<Map<String,Object>> preferences(){return Result.success(service.preferences());}
    @PutMapping("/preferences") public Result<Void> preferences(@RequestBody AccountService.Preferences request){service.savePreferences(request);return Result.success();}
    @PostMapping("/profile-changes") public Result<Void> change(@Valid @RequestBody AccountService.ProfileChange request){service.requestProfileChange(request);return Result.success("资料已提交审核，审核通过后生效");}
    @GetMapping("/profile-changes") public Result<List<Map<String,Object>>> history(){return Result.success(service.profileHistory());}
}
