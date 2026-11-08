package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyMainOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/main-orders") @RequiredArgsConstructor
public class LegacyMainOrderController {
 private final LegacyMainOrderService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable long id){return Result.success(service.detail(id));}
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(required=false) Integer status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.list(status,page,size));}
 @GetMapping("/choices/{kind}") public Result<Map<String,Object>> choices(@PathVariable String kind,@RequestParam(required=false) String q,@RequestParam(required=false) Long selectedId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.choices(kind,q,selectedId,page,size));}
 @GetMapping("/group-number/{id}") public Result<Map<String,Object>> groupNumber(@PathVariable long id){return Result.success(service.groupNumber(id));}
 @GetMapping("/hospital-settlement/{id}") public Result<Map<String,Object>> hospitalSettlement(@PathVariable long id){return Result.success(service.hospitalSettlement(id));}
 @PostMapping public Result<Long> create(@RequestBody LegacyMainOrderService.CreateCommand command){return Result.success(service.create(command));}
 @PutMapping("/{id}") public Result<Long> update(@PathVariable long id,@RequestBody LegacyMainOrderService.Command command){return Result.success(service.update(id,command));}
}
