package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.ArticleAdminService;
import com.petlis.service.impl.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/articles") @RequiredArgsConstructor
public class ArticleAdminController {
 private final ArticleAdminService service;
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(required=false)String status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.list(status,page,size));}
 @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable long id){return Result.success(service.detail(id));}
 @PostMapping public Result<Long> draft(@RequestBody ArticleService.Article article,@RequestHeader("Idempotency-Key")String key){return Result.success(service.draft(article,key));}
 @PutMapping("/{id}") public Result<Void> edit(@PathVariable long id,@RequestBody ArticleAdminService.Edit command){service.edit(id,command);return Result.success();}
 @PostMapping("/{id}/status") public Result<Void> status(@PathVariable long id,@RequestBody ArticleAdminService.Change command){service.change(id,command);return Result.success();}
}
