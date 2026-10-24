package com.petlis.entity.business;

import com.petlis.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 订单状态变化通知记录实体类
 * 对应数据库表: lin_lb_notification
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "lin_lb_notification")
public class NotificationRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /**
     * 订单ID (DiagnosisDetailId)
     */
    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    /**
     * 医院ID (InfoHospitalId)
     */
    @Column(name = "hospital_id", nullable = false)
    private Integer hospitalId;

    /**
     * 原状态 (DiagnosisStatus)
     */
    @Column(name = "old_status")
    private Integer oldStatus;

    /**
     * 新状态 (DiagnosisStatus)
     */
    @Column(name = "new_status", nullable = false)
    private Integer newStatus;

    /**
     * 宠物名称
     */
    @Column(name = "animal_name", length = 250)
    private String animalName;

    /**
     * 主人名称
     */
    @Column(name = "master_name", length = 250)
    private String masterName;

    /**
     * 通知消息内容
     */
    @Column(name = "notification_message", columnDefinition = "TEXT")
    private String notificationMessage;

    /**
     * 操作人ID (lin_user.id)
     */
    @Column(name = "operator_id")
    private Integer operatorId;

    /**
     * 操作时间
     */
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    /**
     * 微信发送状态 0-待发送 1-已发送 2-失败
     */
    @Column(name = "wechat_sent_status", nullable = false)
    private Integer wechatSentStatus = 0;

    /**
     * 微信消息ID
     */
    @Column(name = "wechat_message_id", length = 100)
    private String wechatMessageId;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Override
    public Integer getId() {
        return id;
    }
}
