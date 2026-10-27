package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.MonthlyBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"bills", "admin/bills"})
@RequiredArgsConstructor
public class MonthlyBillController {
    private final MonthlyBillService service;

    @GetMapping public Result<Map<String, Object>> list(@RequestParam(required = false) String month, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean arrears, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.list(month, status, arrears, page, size));
    }
    @GetMapping("/{id}") public Result<Map<String, Object>> get(@PathVariable long id) { return Result.success(service.get(id)); }
    @GetMapping("/{id}/items") public Result<Map<String, Object>> items(@PathVariable long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) { return Result.success(service.items(id, page, size)); }
    @PostMapping public Result<Map<String, Object>> generate(@RequestBody GenerateCommand command) { return Result.success(service.generate(command.hospitalId(), command.laboratoryId(), command.month())); }
    @PostMapping("/{id}/reconciliation") public Result<Map<String, Object>> reconcile(@PathVariable long id, @RequestBody ReconcileCommand command) { return Result.success(service.reconcile(id, command.action(), command.reason())); }
    @PostMapping("/{id}/settlement") public Result<Map<String, Object>> settle(@PathVariable long id, @RequestBody SettleCommand command) { return Result.success(service.settle(id, command.reference())); }
    @PostMapping("/{id}/overdue-reminder") public Result<Void> remind(@PathVariable long id) { service.remindOverdue(id); return Result.success(); }

    public record GenerateCommand(Integer hospitalId, Integer laboratoryId, String month) { }
    public record ReconcileCommand(String action, String reason) { }
    public record SettleCommand(String reference) { }
}
