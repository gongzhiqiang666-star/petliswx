package com.petlis.common.enums;

import lombok.Getter;

/** 保留现有数据库状态码，按“提交、审核、结算、检测”流转。 */
@Getter
public enum OrderStatusEnum {
    DRAFT(-2, "草稿"), ALL(-1, "全部"), PENDING_PAYMENT(-3, "待支付"),
    PENDING_REVIEW(0, "待审核"), REVIEW_FAILED(1, "审核未通过"),
    WAITING_REPORT(2, "等待报告"), COMPLETED(3, "已完成"), CANCELLED(4, "已取消");

    private final Integer code;
    private final String description;
    OrderStatusEnum(Integer code, String description) { this.code = code; this.description = description; }
    public static OrderStatusEnum fromCode(Integer code) {
        for (OrderStatusEnum s : values()) if (s.code.equals(code)) return s;
        return null;
    }
    public static boolean isValidTransition(OrderStatusEnum from, OrderStatusEnum to) {
        if (from == null || to == null || to == ALL) return false;
        return switch (from) {
            case DRAFT -> to == PENDING_REVIEW || to == CANCELLED;
            case PENDING_REVIEW -> to == REVIEW_FAILED || to == PENDING_PAYMENT || to == WAITING_REPORT || to == CANCELLED;
            case REVIEW_FAILED -> to == PENDING_REVIEW || to == CANCELLED;
            case PENDING_PAYMENT -> to == WAITING_REPORT || to == CANCELLED;
            case WAITING_REPORT -> to == COMPLETED;
            default -> false;
        };
    }
    public static boolean isDraftStatus(Integer code) { return DRAFT.code.equals(code); }
}
