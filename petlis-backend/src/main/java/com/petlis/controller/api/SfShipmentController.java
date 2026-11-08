package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.SfShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("logistics/sf/orders")
@RequiredArgsConstructor
public class SfShipmentController {
    private final SfShipmentService sf;
    private final com.petlis.service.impl.SfLabelService labels;
    @GetMapping("/{orderId}/label")
    public Result<Map<String,Object>> label(@PathVariable int orderId) {return Result.success(labels.get(orderId));}
    @PostMapping("/{orderId}/label")
    public Result<Map<String,Object>> generateLabel(@PathVariable int orderId) {return Result.success(labels.generate(orderId));}
    @GetMapping("/{orderId}")
    public Result<Map<String,Object>> status(@PathVariable int orderId) {return Result.success(sf.status(orderId));}
    @PostMapping("/{orderId}")
    public Result<Map<String,Object>> create(@PathVariable int orderId,@RequestBody SfShipmentService.CreateCommand command) {return Result.success(sf.create(orderId,command));}
    @GetMapping("/{orderId}/reship-options")
    public Result<Map<String,Object>> reshipOptions(@PathVariable int orderId){return Result.success(sf.reshipOptions(orderId));}
    @PostMapping("/{orderId}/reship")
    public Result<Map<String,Object>> reship(@PathVariable int orderId,@RequestBody SfShipmentService.ReshipCommand command){return Result.success(sf.reship(orderId,command));}
    @PostMapping("/{orderId}/cancel")
    public Result<Map<String,Object>> cancel(@PathVariable int orderId) {return Result.success(sf.cancel(orderId));}
    @PostMapping("/{orderId}/routes/refresh")
    public Result<Map<String,Object>> routes(@PathVariable int orderId) {return Result.success(sf.refreshRoutes(orderId));}
    @PostMapping("/{orderId}/reconcile")
    public Result<Map<String,Object>> reconcile(@PathVariable int orderId) {return Result.success(sf.reconcile(orderId));}
}
