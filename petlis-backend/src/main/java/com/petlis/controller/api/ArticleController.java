package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("articles") @RequiredArgsConstructor
public class ArticleController {
 private final ArticleService service;
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(required=false)String category,@RequestParam(defaultValue="false")boolean bookmarked,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.list(category,bookmarked,page,size));}
 @GetMapping("/categories") public Result<List<Map<String,Object>>> categories(){return Result.success(service.categories());}
 @GetMapping("/popup") public Result<Map<String,Object>> popup(){return Result.success(service.popup());}
 @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable long id){return Result.success(service.detail(id));}
 public record Action(boolean liked,boolean bookmarked){}
 @PutMapping("/{id}/action") public Result<Void> action(@PathVariable long id,@RequestBody Action r){service.action(id,r.liked(),r.bookmarked());return Result.success();}
 @PostMapping public Result<Long> publish(@RequestBody ArticleService.Article r){return Result.success(service.publish(r));}
}
