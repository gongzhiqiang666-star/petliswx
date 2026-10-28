package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.common.security.UserRoleAssignment;
import com.petlis.service.impl.AdminService;
import com.petlis.service.impl.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService service;

    @GetMapping("/users")
    public Result<Map<String, Object>> users(@RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) String role, @RequestParam(required = false) Integer hospitalId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.users(status, role, hospitalId, page, size));
    }

    @GetMapping("/users/{id}/roles")
    public Result<List<UserRoleAssignment>> roles(@PathVariable int id) { return Result.success(service.roles(id)); }

    @PostMapping("/users/{id}/roles")
    public Result<Void> assignRole(@PathVariable int id, @RequestBody RoleCommand command) {
        service.assignRole(id, command.roleCode(), command.hospitalId(), command.laboratoryId());
        return Result.success("角色已保存");
    }

    @DeleteMapping("/users/{id}/roles/{assignmentId}")
    public Result<Void> removeRole(@PathVariable int id, @PathVariable long assignmentId) {
        service.removeRole(id, assignmentId);
        return Result.success("角色已移除");
    }

    @PatchMapping("/users/{id}/status")
    public Result<Void> status(@PathVariable int id, @RequestBody StatusCommand command) {
        service.setUserStatus(id, command.active(), command.reason());
        return Result.success(command.active() ? "用户已启用" : "用户已停用");
    }

    @PatchMapping("/users/{id}/review")
    public Result<Void> review(@PathVariable int id, @RequestBody ReviewService.Decision decision) {
        service.reviewUser(id, decision);
        return Result.success("审核结果已保存");
    }

    @GetMapping("/hospitals")
    public Result<Map<String, Object>> hospitals(@RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.hospitals(status, page, size));
    }

    @GetMapping("/hospitals/{id}")
    public Result<Map<String, Object>> hospital(@PathVariable int id) { return Result.success(service.hospital(id)); }

    @PatchMapping("/hospitals/{id}/review")
    public Result<Void> reviewHospital(@PathVariable int id, @RequestBody ReviewService.Decision decision) {
        service.reviewHospital(id, decision);
        return Result.success("医院状态已保存");
    }

    public record RoleCommand(String roleCode, Integer hospitalId, Integer laboratoryId) { }
    public record StatusCommand(boolean active, String reason) { }
}
