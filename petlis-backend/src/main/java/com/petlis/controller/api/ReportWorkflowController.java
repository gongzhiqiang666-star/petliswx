package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.ReportWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("report-workflows")
@RequiredArgsConstructor
public class ReportWorkflowController {
    private final ReportWorkflowService reports;

    @PostMapping
    public Result<List<Long>> generate(@RequestBody GenerateCommand command) {
        return Result.success(reports.generate(command.taskId(), command.summary(), command.conclusion()));
    }

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(reports.list(status, page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable long id) { return Result.success(reports.get(id)); }

    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable long id, @RequestBody ReviewCommand command) {
        reports.review(id, command.decision(), command.reason());
        return Result.success("报告审核结果已保存");
    }

    @PostMapping("/{id}/publish")
    public Result<Map<String, Object>> publish(@PathVariable long id) {
        return Result.success("报告已发布", reports.publish(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id) { return reports.download(id); }

    @PutMapping("/templates/{infoTestTypeId}")
    public Result<Long> template(@PathVariable int infoTestTypeId, @RequestBody TemplateCommand command) {
        return Result.success(reports.saveTemplate(infoTestTypeId, command.laboratoryId(), command.templateName(), command.templateCode(), command.content()));
    }

    public record GenerateCommand(long taskId, String summary, String conclusion) { }
    public record ReviewCommand(String decision, String reason) { }
    public record TemplateCommand(int laboratoryId, String templateName, String templateCode, String content) { }
}
