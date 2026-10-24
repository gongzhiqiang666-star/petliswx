package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知记录响应DTO
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "通知记录响应")
public class NotificationResponse {

    @Schema(description = "通知ID")
    private Integer id;

    @Schema(description = "订单ID")
    private Integer orderId;

    @Schema(description = "医院ID")
    private Integer hospitalId;

    @Schema(description = "医院名称")
    private String hospitalName;

    @Schema(description = "原状态")
    private Integer oldStatus;

    @Schema(description = "原状态描述")
    private String oldStatusLabel;

    @Schema(description = "新状态")
    private Integer newStatus;

    @Schema(description = "新状态描述")
    private String newStatusLabel;

    @Schema(description = "宠物名称")
    private String animalName;

    @Schema(description = "主人名称")
    private String masterName;

    @Schema(description = "通知消息内容")
    private String notificationMessage;

    @Schema(description = "操作人ID")
    private Integer operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;

    @Schema(description = "微信发送状态 0-待发送 1-已发送 2-失败")
    private Integer wechatSentStatus;

    @Schema(description = "微信发送状态描述")
    private String wechatSentStatusLabel;

    @Schema(description = "微信消息ID")
    private String wechatMessageId;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private Integer createtime;

    @Schema(description = "更新时间")
    private Integer updatetime;
}
