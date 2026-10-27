package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.ShipmentWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("logistics")
@RequiredArgsConstructor
public class LogisticsWorkflowController {
    private final ShipmentWorkflowService shipments;

    @PostMapping
    public Result<Long> create(@RequestBody CreateCommand command) {
        var value = command.shipment();
        return Result.success(shipments.create(command.orderId(), value));
    }

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(shipments.list(status, page, size));
    }

    @GetMapping({"/orders", "/orders/list"})
    public Result<Map<String, Object>> orderList(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return list(status, page, size);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable long id) { return Result.success(shipments.get(id)); }

    @GetMapping("/orders/{orderId}")
    public Result<Map<String, Object>> getByOrder(@PathVariable int orderId) { return Result.success(shipments.getByOrder(orderId)); }

    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable long id, @RequestBody AssignCommand command) {
        shipments.assign(id, command.courierUserId());
        return Result.success("运单已分配");
    }

    @PostMapping("/{id}/pickup")
    public Result<Map<String, Object>> pickup(@PathVariable long id) { return transition(id, ShipmentWorkflowService.ShipmentAction.PICKUP); }

    @PostMapping("/{id}/transit")
    public Result<Map<String, Object>> transit(@PathVariable long id) { return transition(id, ShipmentWorkflowService.ShipmentAction.TRANSIT); }

    @PostMapping("/{id}/deliver")
    public Result<Map<String, Object>> deliver(@PathVariable long id) { return transition(id, ShipmentWorkflowService.ShipmentAction.DELIVER); }

    @PostMapping("/{id}/receive")
    public Result<Map<String, Object>> receive(@PathVariable long id) { return transition(id, ShipmentWorkflowService.ShipmentAction.RECEIVE); }

    @PostMapping("/{id}/exception")
    public Result<Map<String, Object>> exception(@PathVariable long id, @RequestBody ExceptionCommand command) {
        String note = command.note() == null || command.note().isBlank() ? command.reason() : command.note();
        return Result.success(shipments.transition(id, ShipmentWorkflowService.ShipmentAction.EXCEPTION, command.exceptionType(), note));
    }

    private Result<Map<String, Object>> transition(long id, ShipmentWorkflowService.ShipmentAction action) {
        return Result.success(shipments.transition(id, action, null, null));
    }

    public record CreateCommand(int orderId, String trackingNo, Integer laboratoryId, String pickupAddress,
                                String expectedArrivedAt, Integer courierUserId) {
        ShipmentWorkflowService.ShipmentCommand shipment() {
            return new ShipmentWorkflowService.ShipmentCommand(trackingNo, laboratoryId, pickupAddress, expectedArrivedAt, courierUserId);
        }
    }
    public record AssignCommand(int courierUserId) { }
    public record ExceptionCommand(String exceptionType, String note, String reason) { }
}
