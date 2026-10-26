package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LaboratoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("laboratories") @RequiredArgsConstructor
public class LaboratoryController {
 private final LaboratoryService service;
 @GetMapping public Result<List<Map<String,Object>>> list(){return Result.success(service.list());}
 @GetMapping("/{id}/tests") public Result<Map<String,Object>> tests(@PathVariable int id,@RequestParam(defaultValue="")String search,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.tests(id,search,page,size));}
 @PostMapping public Result<Long> create(@RequestBody LaboratoryService.Laboratory r){return Result.success(service.create(r));}
 public record TestPrice(java.math.BigDecimal price){}
 @PutMapping("/{id}/tests/{testId}") public Result<Void> assign(@PathVariable int id,@PathVariable int testId,@RequestBody TestPrice r){service.assignTest(id,testId,r.price());return Result.success();}
}
