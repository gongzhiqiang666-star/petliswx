package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyDictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("admin/legacy") @RequiredArgsConstructor
public class LegacyDictionaryController {
 private final LegacyDictionaryService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/dictionaries") public Result<List<Map<String,Object>>> schemas(){return Result.success(service.schemas());}
 @GetMapping("/dictionaries/{kind}") public Result<Map<String,Object>> list(@PathVariable String kind,
  @RequestParam(required=false) String name,@RequestParam(required=false) String englishName,@RequestParam(required=false) Integer active,
  @RequestParam(required=false) Long createdFrom,@RequestParam(required=false) Long createdTo,@RequestParam(required=false) Long updatedFrom,@RequestParam(required=false) Long updatedTo,
  @RequestParam(required=false) String sort,@RequestParam(required=false) String order,
  @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  return Result.success(service.list(kind,new LegacyDictionaryService.Query(name,englishName,active,createdFrom,createdTo,updatedFrom,updatedTo,sort,order,page,size)));
 }

 @GetMapping("/dictionaries/{kind}/select") public Result<Map<String,Object>> select(@PathVariable String kind,
  @RequestParam(required=false) List<String> words,@RequestParam(required=false) List<String> searchFields,
  @RequestParam(required=false) String andOr,@RequestParam(required=false) List<Long> selectedIds,
  @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,@RequestParam(required=false) Integer active){
  return Result.success(service.select(kind,words,searchFields,andOr,selectedIds,page,size,active));
 }

 @GetMapping("/dictionaries/{kind}/{id}") public Result<Map<String,Object>> get(@PathVariable String kind,@PathVariable long id){return Result.success(service.get(kind,id));}
 @PostMapping("/dictionaries/{kind}") public Result<Long> create(@PathVariable String kind,@RequestBody LegacyDictionaryService.Command command){return Result.success(service.create(kind,command));}
 @PutMapping("/dictionaries/{kind}/{id}") public Result<Long> update(@PathVariable String kind,@PathVariable long id,@RequestBody LegacyDictionaryService.Command command){return Result.success(service.update(kind,id,command));}

 @PostMapping("/dictionaries/{kind}/delete") public Result<Map<String,Integer>> delete(@PathVariable String kind,@RequestBody LegacyDictionaryService.BatchCommand command){return Result.success(Map.of("count",service.delete(kind,command)));}
 @PostMapping("/dictionaries/{kind}/status") public Result<Map<String,Integer>> status(@PathVariable String kind,@RequestBody LegacyDictionaryService.BatchCommand command){return Result.success(Map.of("count",service.status(kind,command)));}
}
