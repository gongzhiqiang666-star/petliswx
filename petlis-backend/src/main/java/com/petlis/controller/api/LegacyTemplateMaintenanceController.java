package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyTemplateMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/templates") @RequiredArgsConstructor
public class LegacyTemplateMaintenanceController {
 private final LegacyTemplateMaintenanceService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/projects") public Result<Map<String,Object>> projects(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="100") int size){return Result.success(service.projects(q,page,size));}
 @GetMapping("/{kind}") public Result<Map<String,Object>> list(@PathVariable String kind,@RequestParam(required=false) Long projectId,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.list(kind,projectId,q,page,size));}
 @GetMapping("/{kind}/{id}") public Result<Map<String,Object>> detail(@PathVariable String kind,@PathVariable long id){return Result.success(service.detail(kind,id));}
 @PostMapping("/{kind}") public Result<Long> create(@PathVariable String kind,@RequestBody LegacyTemplateMaintenanceService.Command command){return Result.success(service.save(kind,null,command));}
 @PutMapping("/{kind}/{id}") public Result<Long> update(@PathVariable String kind,@PathVariable long id,@RequestBody LegacyTemplateMaintenanceService.Command command){return Result.success(service.save(kind,id,command));}
 @PostMapping("/{kind}/{id}/delete") public Result<Long> delete(@PathVariable String kind,@PathVariable long id,@RequestBody LegacyTemplateMaintenanceService.DeleteCommand command){return Result.success(service.delete(kind,id,command));}
}
