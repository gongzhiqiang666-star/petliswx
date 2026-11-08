package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyHospitalHistoryService;
import com.petlis.service.impl.LegacyHospitalCancellationService;
import com.petlis.service.impl.LegacyHospitalDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("orders/hospital-history")
@RequiredArgsConstructor
public class LegacyHospitalHistoryController {
    private final LegacyHospitalHistoryService service;
    private final LegacyHospitalCancellationService cancellations;
    private final LegacyHospitalDraftService drafts;
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
