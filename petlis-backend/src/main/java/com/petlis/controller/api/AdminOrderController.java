package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final AdminOrderService service;
    @GetMapping public Result<Map<String, Object>> list(@RequestParam(required = false) Integer status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return Result.success(service.list(status, page, size)); }
}
