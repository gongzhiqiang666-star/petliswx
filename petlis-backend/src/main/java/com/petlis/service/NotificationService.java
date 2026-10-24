package com.petlis.service;

import com.petlis.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;

/**
 * 通知Service接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
public interface NotificationService {

    /**
     * 查询订单的通知记录列表
     *
     * @param orderId 订单ID
     * @return 通知记录列表
     */
    Page<NotificationResponse> getOrderNotifications(Integer orderId);

    /**
     * 查询医院的通知记录列表(分页)
     *
     * @param hospitalId 医院ID
     * @param pageNum    页码
     * @param pageSize   每页大小
     * @return 通知记录分页结果
     */
    Page<NotificationResponse> getHospitalNotifications(Integer hospitalId, Integer pageNum, Integer pageSize);

    /**
     * 获取通知详情
     *
     * @param notificationId 通知ID
     * @return 通知详情
     */
    NotificationResponse getNotificationDetail(Integer notificationId);

    /**
     * 处理待发送的微信通知
     * 此方法由定时任务调用
     *
     * @return 处理的通知数量
     */
    Integer processPendingNotifications();

    /**
     * 重试发送失败的通知
     *
     * @param notificationId 通知ID
     * @return 是否重试成功
     */
    Boolean retryNotification(Integer notificationId);

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     */
    void markAsRead(Integer notificationId);

    /**
     * 统计医院未读通知数量
     *
     * @param hospitalId 医院ID
     * @return 未读通知数量
     */
    Long countUnreadNotifications(Integer hospitalId);
}
