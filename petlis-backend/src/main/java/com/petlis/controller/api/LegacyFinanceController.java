package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/finance") @RequiredArgsConstructor
public class LegacyFinanceController {
 private final LegacyFinanceService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(required=false) Long hospital,@RequestParam(defaultValue="") String orderNo,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return Result.success(service.list(hospital,orderNo,page,size));}
 @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable long id){return Result.success(service.detail(id));}
 @PostMapping("/batch") public Result<Integer> batch(@RequestBody LegacyFinanceService.BatchCommand command){return Result.success(service.batch(command));}
 @PostMapping("/{id}/writeoff") public Result<Long> writeoff(@PathVariable long id,@RequestBody LegacyFinanceService.WriteoffCommand command){return Result.success(service.writeoff(id,command));}
}
