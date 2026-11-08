package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyHospitalHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("orders/hospital-history")
@RequiredArgsConstructor
public class LegacyHospitalHistoryController {
    private final LegacyHospitalHistoryService service;
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
