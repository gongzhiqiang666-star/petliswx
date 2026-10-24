package com.petlis.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 *
 * <p>订单状态对应数据库表 lin_lb_diagnosisdetail.DiagnosisStatus 字段</p>
 *
 * <p>状态流转规则:</p>
 * <ul>
 *   <li>草稿(-2) → 待审核(0) 或 待支付(-3) 或 已取消(4)</li>
 *   <li>待支付(-3) → 待审核(0) (支付成功后) 或 已取消(4)</li>
 *   <li>待审核(0) → 审核未通过(1) 或 等待报告(2)</li>
 *   <li>等待报告(2) → 已完成(3)</li>
 *   <li>已完成(3) 和 已取消(4) 为终态，不可流转</li>
 * </ul>
 *
 * <p>特殊状态:</p>
 * <ul>
 *   <li>全部(-1): 仅用于查询过滤，不作为实际订单状态</li>
 * </ul>
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Getter
public enum OrderStatusEnum {

    /**
     * 草稿状态
     * 订单创建后的初始状态，用户可以编辑订单信息
     */
    DRAFT(-2, "草稿"),

    /**
     * 全部
     * 特殊状态，仅用于前端查询过滤（显示所有订单），不作为实际订单状态存储
     */
    ALL(-1, "全部"),

    /**
     * 待支付（草稿）
     * 次结客户订单创建后的初始状态，等待用户完成支付
     * 支付成功后流转到待审核(0)状态
     */
    PENDING_PAYMENT(-3, "草稿"),

    /**
     * 待审核
     * 订单已提交/支付已完成，等待实验室审核
     * - 月结客户：订单创建后直接进入此状态
     * - 次结客户：支付成功后流转到此状态
     */
    PENDING_REVIEW(0, "待审核"),

    /**
     * 审核未通过
     * 订单审核失败，需要用户修改后重新提交
     */
    REVIEW_FAILED(1, "审核未通过"),

    /**
     * 等待报告
     * 订单已确认，实验室正在进行检测，等待报告生成
     */
    WAITING_REPORT(2, "等待报告"),

    /**
     * 已完成
     * 检测完成，报告已生成，订单结束
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消
     * 订单已取消，不可恢复
     */
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String description;

    OrderStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 订单状态枚举
     */
    public static OrderStatusEnum fromCode(Integer code) {
        for (OrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 验证状态流转是否合法
     *
     * @param from 当前状态
     * @param to 目标状态
     * @return 是否合法
     */
    public static boolean isValidTransition(OrderStatusEnum from, OrderStatusEnum to) {
        // 草稿可以转为待审核或待支付或已取消
        if (from == DRAFT) {
            return to == PENDING_REVIEW || to == PENDING_PAYMENT || to == CANCELLED;
        }

        // 待支付只能转为待审核（支付成功）或已取消
        if (from == PENDING_PAYMENT) {
            return to == PENDING_REVIEW || to == CANCELLED;
        }

        // 待审核可以转为审核不通过或等待报告
        if (from == PENDING_REVIEW) {
            return to == REVIEW_FAILED || to == WAITING_REPORT;
        }

        // 等待报告只能转为已完成
        if (from == WAITING_REPORT) {
            return to == COMPLETED;
        }

        // 已完成和已取消不能流转
        if (from == COMPLETED || from == CANCELLED) {
            return false;
        }

        return false;
    }

    /**
     * 判断是否为草稿状态（包含 -3 待支付和 -2 草稿）
     *
     * @param code 状态码
     * @return 是否为草稿状态
     */
    public static boolean isDraftStatus(Integer code) {
        return code != null && (code.equals(DRAFT.getCode()) || code.equals(PENDING_PAYMENT.getCode()));
    }
}
