package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;
    @GetMapping("/dashboard") public Result<Map<String, Object>> dashboard(@RequestParam String from, @RequestParam String to) { return Result.success(service.dashboard(from, to)); }
    @GetMapping("/export/orders.csv") public ResponseEntity<byte[]> export(@RequestParam String from, @RequestParam String to) {
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv").body(service.exportOrders(from, to).getBytes(StandardCharsets.UTF_8));
    }
}
