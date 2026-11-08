package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyHospitalHistoryService;
import com.petlis.service.impl.LegacyHospitalCancellationService;
import com.petlis.service.impl.LegacyHospitalDraftService;
import com.petlis.service.impl.LegacyHospitalDraftItemsService;
import com.petlis.service.impl.LegacyHospitalDraftSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("orders/hospital-history")
@RequiredArgsConstructor
public class LegacyHospitalHistoryController {
    private final LegacyHospitalHistoryService service;
    private final com.petlis.service.impl.LegacyHospitalReportService reports;
    @GetMapping("/reports/{detailId}/pdf") public org.springframework.http.ResponseEntity<?> report(@PathVariable long detailId,@RequestParam int hospitalId) { return reports.download(detailId,hospitalId); }
    private final LegacyHospitalCancellationService cancellations;
    private final LegacyHospitalDraftService drafts;
    private final LegacyHospitalDraftItemsService items;
    private final LegacyHospitalDraftSubmissionService submissions;
    @GetMapping("/hospitals") public Result<Map<String,Object>> hospitals(@RequestParam(required=false) String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return Result.success(service.hospitals(q,page,size)); }
    @GetMapping("/drafts/{id}/preview") public Result<Map<String,Object>> preview(@PathVariable long id,@RequestParam int hospitalId) { return Result.success(submissions.preview(id,hospitalId)); }
    @PostMapping("/drafts/{id}/submit") public Result<Map<String,Object>> submit(@PathVariable long id,@RequestBody LegacyHospitalDraftSubmissionService.Command command) { return Result.success(submissions.submit(id,command)); }
    @GetMapping("/drafts/{id}/items") public Result<Map<String,Object>> selected(@PathVariable long id,@RequestParam int hospitalId) { return Result.success(items.selected(id,hospitalId)); }
    @GetMapping("/drafts/{id}/test-catalog") public Result<Map<String,Object>> catalog(@PathVariable long id,@RequestParam int hospitalId,@RequestParam(required=false) String q,@RequestParam(required=false) Integer category,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return Result.success(items.catalog(id,hospitalId,q,category,page,size)); }
    @PostMapping("/drafts/{id}/items/add") public Result<Long> addItem(@PathVariable long id,@RequestBody LegacyHospitalDraftItemsService.Command command) { return Result.success(items.add(id,command)); }
    @PostMapping("/drafts/{id}/items/remove") public Result<Long> removeItem(@PathVariable long id,@RequestBody LegacyHospitalDraftItemsService.Command command) { return Result.success(items.remove(id,command)); }
    @PostMapping("/drafts") public Result<Long> createDraft(@RequestBody LegacyHospitalDraftService.CreateCommand command) { return Result.success(drafts.create(command)); }
    @GetMapping("/drafts") public Result<Map<String,Object>> drafts(@RequestParam int hospitalId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) { return Result.success(drafts.list(hospitalId,page,size)); }
    @GetMapping("/drafts/{id}") public Result<Map<String,Object>> draft(@PathVariable long id,@RequestParam int hospitalId) { return Result.success(drafts.detail(id,hospitalId)); }
    @PutMapping("/drafts/{id}") public Result<Long> saveDraft(@PathVariable long id,@RequestBody LegacyHospitalDraftService.Command command) { return Result.success(drafts.update(id,command)); }
    @GetMapping("/draft-choices/{kind}") public Result<Map<String,Object>> choices(@PathVariable String kind,@RequestParam int hospitalId,@RequestParam(required=false) String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) { return Result.success(drafts.choices(hospitalId,kind,q,page,size)); }
    @PostMapping("/{orderId}/cancel")
    public Result<Map<String,Object>> cancel(@PathVariable long orderId,@RequestBody LegacyHospitalCancellationService.Command command) { return Result.success(cancellations.cancel(orderId,command)); }
    @GetMapping("/capabilities")
    public Result<Map<String, Boolean>> capabilities() { return Result.success(service.capabilities()); }
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam Integer hospitalId,
            @RequestParam(required=false) String userNo, @RequestParam(required=false) Integer status,
            @RequestParam(required=false) String from, @RequestParam(required=false) String to,
            @RequestParam(defaultValue="0") Integer page, @RequestParam(defaultValue="10") Integer size) {
        return Result.success(service.list(new LegacyHospitalHistoryService.Query(hospitalId,userNo,status,from,to,page,size)));
    }
}
