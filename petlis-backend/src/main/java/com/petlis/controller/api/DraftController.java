package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.DraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("drafts") @RequiredArgsConstructor
public class DraftController {
 private final DraftService service;
 @GetMapping public Result<List<Map<String,Object>>> list(){return Result.success(service.list());}
 @GetMapping("/{id}") public Result<Map<String,Object>> get(@PathVariable long id){return Result.success(service.get(id));}
 @PostMapping public Result<Long> create(@RequestBody DraftService.Draft r){return Result.success(service.create(r));}
 @PutMapping("/{id}") public Result<Void> update(@PathVariable long id,@RequestBody DraftService.Draft r){service.update(id,r);return Result.success();}
 @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable long id){service.delete(id);return Result.success();}
}
