package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyHospitalFundsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("admin/legacy/hospital-funds") @RequiredArgsConstructor
public class LegacyHospitalFundsController {
 private final LegacyHospitalFundsService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/{id}") public Result<Map<String,Object>> summary(@PathVariable long id){return Result.success(service.summary(id));}
 @GetMapping("/{id}/entries") public Result<Map<String,Object>> entries(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.entries(id,page,size));}
 @PostMapping("/{id}/entries") public Result<Long> adjust(@PathVariable long id,@RequestBody LegacyHospitalFundsService.Command command){return Result.success(service.adjust(id,command));}
}
