package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.OperationalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("admin/operations")
@RequiredArgsConstructor
public class OperationalController {
    private final OperationalService service;
    @GetMapping("/audits") public Result<Map<String, Object>> audits(@RequestParam(required = false) String action, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return Result.success(service.audits(action, page, size)); }
    @GetMapping("/audits/{id}") public Result<Map<String, Object>> audit(@PathVariable long id) { return Result.success(service.audit(id)); }
    @GetMapping("/configs") public Result<Map<String, Object>> configs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return Result.success(service.configs(page, size)); }
    @PostMapping("/configs") public Result<Long> create(@RequestBody ConfigCreate command) { return Result.success(service.createConfig(command.key(), command.value(), command.description())); }
    @PutMapping("/configs/{key}") public Result<Void> update(@PathVariable String key, @RequestBody ConfigUpdate command) { service.saveConfig(key, command.value(), command.version(), command.reason()); return Result.success(); }
    @PostMapping("/points-adjustments") public Result<Void> points(@RequestBody PointsCommand command) { service.adjustPoints(command.userId(), command.delta(), command.reason(), command.requestKey()); return Result.success(); }
    public record ConfigCreate(String key, Map<String, Object> value, String description) { }
    public record ConfigUpdate(Map<String, Object> value, int version, String reason) { }
    public record PointsCommand(Integer userId, Integer delta, String reason, String requestKey) { }
}
