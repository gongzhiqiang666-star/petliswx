package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"contracts", "admin/contracts"})
@RequiredArgsConstructor
public class ContractController {
    private final ContractService service;

    @GetMapping public Result<Map<String, Object>> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return Result.success(service.list(page, size)); }
    @GetMapping("/{id}") public Result<Map<String, Object>> get(@PathVariable long id) { return Result.success(service.get(id)); }
    @PostMapping public Result<Long> create(@RequestBody ContractService.Command command) { return Result.success(service.create(command)); }
    @PutMapping("/{id}") public Result<Void> update(@PathVariable long id, @RequestParam int version, @RequestBody ContractService.Command command) { service.update(id, version, command); return Result.success(); }
    @PostMapping("/{id}/prices") public Result<Long> savePrice(@PathVariable long id, @RequestBody ContractService.PriceCommand command) { return Result.success(service.savePrice(id, command)); }
    @PatchMapping("/{id}/status") public Result<Void> status(@PathVariable long id, @RequestBody StatusCommand command) { service.setStatus(id, command.active(), command.reason()); return Result.success(command.active() ? "合同已启用" : "合同已停用"); }
    public record StatusCommand(boolean active, String reason) { }
}
