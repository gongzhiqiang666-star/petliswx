package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.LabWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("lab/tasks")
@RequiredArgsConstructor
public class LabWorkflowController {
    private final LabWorkflowService laboratory;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(laboratory.list(status, page, size));
    }

    @GetMapping("/assignees")
    public Result<Map<String, Object>> assignees(@RequestParam Integer labId,
            @RequestParam(required = false) Integer laboratoryId) {
        return Result.success(laboratory.assignees(labId != null ? labId : laboratoryId));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable long id) { return Result.success(laboratory.get(id)); }

    @PostMapping("/order/{orderId}/assign")
    public Result<Map<String, Object>> assign(@PathVariable int orderId, @RequestBody AssignCommand command) {
        return Result.success(laboratory.assign(orderId, command.assigneeId(), command.equipmentCode()));
    }

    @PostMapping("/{id}/results")
    public Result<Long> results(@PathVariable long id, @RequestBody LabWorkflowService.ResultsCommand command) {
        return Result.success(laboratory.enterResults(id, command));
    }

    @PostMapping("/{id}/return")
    public Result<Void> returnSample(@PathVariable long id, @RequestBody ReasonCommand command) {
        laboratory.returnSample(id, command.reason());
        return Result.success("样本已退回");
    }

    @PostMapping("/{id}/retest")
    public Result<Long> retest(@PathVariable long id, @RequestBody ReasonCommand command) {
        return Result.success(laboratory.retest(id, command.reason()));
    }

    public record AssignCommand(int assigneeId, String equipmentCode, String equipmentName) {
        @Override public String equipmentCode() { return equipmentCode == null || equipmentCode.isBlank() ? equipmentName : equipmentCode; }
    }
    public record ReasonCommand(String reason) { }
}
