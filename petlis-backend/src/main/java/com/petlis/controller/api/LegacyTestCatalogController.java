package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyTestCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("admin/legacy/test-catalog") @RequiredArgsConstructor
public class LegacyTestCatalogController {
 private final LegacyTestCatalogService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @PostMapping("/quote") public Result<Map<String,String>> quote(@RequestBody LegacyTestCatalogService.QuoteCommand command){return Result.success(Map.of("price",service.quote(command)));}
 @GetMapping("/items") public Result<Map<String,Object>> items(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.items(page,size));}
 @GetMapping("/items/{id}") public Result<Map<String,Object>> item(@PathVariable long id){return Result.success(service.item(id));}
 @PostMapping("/items") public Result<Long> create(@RequestBody LegacyTestCatalogService.ItemCommand command){return Result.success(service.create(command));}
 @PutMapping("/items/{id}") public Result<Long> update(@PathVariable long id,@RequestBody LegacyTestCatalogService.ItemCommand command){return Result.success(service.update(id,command));}
 @GetMapping("/items/{id}/prices") public Result<Map<String,Object>> prices(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.prices(id,page,size));}
 @PostMapping("/items/{id}/prices") public Result<Long> addPrice(@PathVariable long id,@RequestBody LegacyTestCatalogService.PriceCommand command){return Result.success(service.addPrice(id,command));}
 @GetMapping("/items/{id}/prices/{priceId}") public Result<Map<String,Object>> price(@PathVariable long id,@PathVariable long priceId){return Result.success(service.price(id,priceId));}
 @PutMapping("/items/{id}/prices/{priceId}") public Result<Long> updatePrice(@PathVariable long id,@PathVariable long priceId,@RequestBody LegacyTestCatalogService.PriceEditCommand command){return Result.success(service.updatePrice(id,priceId,command));}
 @PostMapping("/items/{id}/prices/{priceId}/delete") public Result<Long> deletePrice(@PathVariable long id,@PathVariable long priceId,@RequestBody LegacyTestCatalogService.PriceDeleteCommand command){return Result.success(service.deletePrice(id,priceId,command));}
}
