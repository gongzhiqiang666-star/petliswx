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
    @PatchMapping("/laboratories/{laboratoryId}/items/{testTypeId}/status") public Result<Void> status(@PathVariable int laboratoryId, @PathVariable int testTypeId, @RequestBody StatusCommand command) { service.setItemStatus(laboratoryId, testTypeId, command.active()); return Result.success(); }
    public record StatusCommand(boolean active) { }
}
