package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.dto.response.NotificationResponse;
import com.petlis.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 通知管理Controller
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "订单状态变化通知的查询和管理")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 查询订单的通知记录列表
     *
     * @param orderId 订单ID
     * @return 通知记录列表
     */
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "查询订单通知记录", description = "查询指定订单的所有通知记录")
    public Result<Page<NotificationResponse>> getOrderNotifications(
            @Parameter(description = "订单ID", required = true) @PathVariable Integer orderId
    ) {
        log.info("接收到查询订单通知记录请求, orderId: {}", orderId);
        try {
            Page<NotificationResponse> result = notificationService.getOrderNotifications(orderId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询订单通知记录失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询当前用户医院的通知记录列表(分页)
     *
     * @param pageNum  页码(从0开始,默认0)
     * @param pageSize 每页大小(默认10)
     * @return 通知记录分页结果
     */
    @GetMapping("/hospitals/my")
    @Operation(summary = "查询当前医院通知记录", description = "查询当前登录用户所在医院的通知记录列表,支持分页")
    public Result<Page<NotificationResponse>> getMyHospitalNotifications(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "0") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        try {
            // 从token中获取医院ID
            Integer hospitalId = com.petlis.common.security.SecurityUtils.getCurrentHospitalId();

            log.info("接收到查询医院通知记录请求, hospitalId: {}, pageNum: {}, pageSize: {}", hospitalId, pageNum, pageSize);
            Page<NotificationResponse> result = notificationService.getHospitalNotifications(hospitalId, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询医院通知记录失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取通知详情
     *
     * @param id 通知ID
     * @return 通知详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取通知详情", description = "获取指定通知记录的详细信息")
    public Result<NotificationResponse> getNotificationDetail(
            @Parameter(description = "通知ID", required = true) @PathVariable Integer id
    ) {
        log.info("接收到获取通知详情请求, id: {}", id);
        try {
            NotificationResponse response = notificationService.getNotificationDetail(id);
            return Result.success(response);
        } catch (Exception e) {
            log.error("获取通知详情失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 重试发送失败的通知
     *
     * @param id 通知ID
     * @return 操作结果
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "重试发送通知", description = "重试发送失败的通知")
    public Result<Boolean> retryNotification(
            @Parameter(description = "通知ID", required = true) @PathVariable Integer id
    ) {
        log.info("接收到重试发送通知请求, id: {}", id);
        try {
            Boolean result = notificationService.retryNotification(id);
            return Result.success("重试发送完成", result);
        } catch (Exception e) {
            log.error("重试发送通知失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 标记通知为已读
     *
     * @param id 通知ID
     * @return 操作结果
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读", description = "将指定通知标记为已读状态")
    public Result<Void> markAsRead(
            @Parameter(description = "通知ID", required = true) @PathVariable Integer id
    ) {
        log.info("接收到标记通知为已读请求, id: {}", id);
        try {
            notificationService.markAsRead(id);
            return Result.success("标记成功");
        } catch (Exception e) {
            log.error("标记通知为已读失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 统计当前用户医院未读通知数量
     *
     * @return 未读通知数量
     */
    @GetMapping("/hospitals/my/unread-count")
    @Operation(summary = "统计当前医院未读通知数量", description = "统计当前登录用户所在医院的未读通知数量")
    public Result<Long> countMyUnreadNotifications() {
        try {
            // 从token中获取医院ID
            Integer hospitalId = com.petlis.common.security.SecurityUtils.getCurrentHospitalId();

            log.info("接收到统计未读通知数量请求, hospitalId: {}", hospitalId);
            Long count = notificationService.countUnreadNotifications(hospitalId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("统计未读通知数量失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 处理待发送的微信通知
     * 此接口由定时任务或管理员调用
     *
     * @return 处理的通知数量
     */
    @PostMapping("/process-pending")
    @Operation(summary = "处理待发送通知", description = "处理所有待发送的微信通知,由定时任务调用")
    public Result<Integer> processPendingNotifications() {
        log.info("接收到处理待发送通知请求");
        try {
            Integer count = notificationService.processPendingNotifications();
            return Result.success("处理完成", count);
        } catch (Exception e) {
            log.error("处理待发送通知失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
