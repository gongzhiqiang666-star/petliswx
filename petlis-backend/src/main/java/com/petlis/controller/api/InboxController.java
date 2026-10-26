package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.BusinessNotifications;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("inbox") @RequiredArgsConstructor
public class InboxController {
 private final BusinessNotifications service;
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(defaultValue="false")boolean unread,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.inbox(unread,page,size));}
 @GetMapping("/unread-count") public Result<Long> count(){return Result.success(service.unread());}
 @GetMapping("/templates") public Result<List<Map<String,Object>>> templates(){return Result.success(service.templates());}
 @PutMapping("/{id}/read") public Result<Void> read(@PathVariable long id){service.read(id);return Result.success();}
 @PutMapping("/read-all") public Result<Void> all(){service.readAll();return Result.success();}
 @GetMapping("/{id}/deliveries") public Result<List<Map<String,Object>>> logs(@PathVariable long id){return Result.success(service.deliveries(id));}
 @PostMapping("/{id}/retry") public Result<Void> retry(@PathVariable long id){service.retry(id);return Result.success();}
}
