package com.petlis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知发送日志实体
 */
@Data
@Entity
@Table(name = "lin_lb_notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 通知记录ID
     */
    @Column(name = "notification_id")
    private Integer notificationId;

    /**
     * 订单ID
     */
    @Column(name = "order_id")
    private Integer orderId;

    /**
     * 医院ID
     */
    @Column(name = "hospital_id")
    private Integer hospitalId;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Integer userId;

    /**
     * 微信OpenID
     */
    @Column(name = "openid")
    private String openid;

    /**
     * 消息模板ID
     */
    @Column(name = "template_id")
    private String templateId;

    /**
     * 微信消息ID
     */
    @Column(name = "message_id")
    private String messageId;

    /**
     * 发送状态 0-待发送 1-成功 2-失败
     */
    @Column(name = "send_status")
    private Integer sendStatus;

    /**
     * 错误码
     */
    @Column(name = "error_code")
    private String errorCode;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 发送时间
     */
    @Column(name = "send_time")
    private LocalDateTime sendTime;

    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount;

    /**
     * 创建时间
     */
    @Column(name = "createtime")
    private Long createtime;

    /**
     * 更新时间
     */
    @Column(name = "updatetime")
    private Long updatetime;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis() / 1000;
        createtime = now;
        updatetime = now;
        if (sendStatus == null) {
            sendStatus = 0;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatetime = System.currentTimeMillis() / 1000;
    }
}
