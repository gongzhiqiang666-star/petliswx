package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.AdminCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final AdminCatalogService service;
    @GetMapping("/laboratories") public Result<Map<String, Object>> laboratories(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) { return Result.success(service.laboratories(page, size)); }
    @GetMapping("/items") public Result<Map<String, Object>> items(@RequestParam int laboratoryId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) { return Result.success(service.items(laboratoryId, page, size)); }
    @PostMapping("/laboratories/{laboratoryId}/items") public Result<Long> create(@PathVariable int laboratoryId, @RequestBody AdminCatalogService.ItemCommand command) { return Result.success(service.createItem(laboratoryId, command)); }
    @PutMapping("/laboratories/{laboratoryId}/items/{testTypeId}") public Result<Void> update(@PathVariable int laboratoryId, @PathVariable int testTypeId, @RequestBody AdminCatalogService.ItemCommand command) { service.updateItem(laboratoryId, testTypeId, command); return Result.success(); }
    @DeleteMapping("/laboratories/{laboratoryId}/items/{testTypeId}") public Result<Void> delete(@PathVariable int laboratoryId, @PathVariable int testTypeId) { service.deleteItem(laboratoryId, testTypeId); return Result.success("项目已逻辑停用"); }
    @PatchMapping("/laboratories/{laboratoryId}/items/{testTypeId}/status") public Result<Void> status(@PathVariable int laboratoryId, @PathVariable int testTypeId, @RequestBody StatusCommand command) { service.setItemStatus(laboratoryId, testTypeId, command.active()); return Result.success(); }
    public record StatusCommand(boolean active) { }
}
