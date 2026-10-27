package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("admin/refunds")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService service;
    @PostMapping("/{orderId}") public Result<Map<String, Object>> refund(@PathVariable int orderId, @RequestBody RefundCommand command) {
        return Result.success(service.refund(orderId, command.reason(), command.requestKey()));
    }
    public record RefundCommand(String reason, String requestKey) { }
}
