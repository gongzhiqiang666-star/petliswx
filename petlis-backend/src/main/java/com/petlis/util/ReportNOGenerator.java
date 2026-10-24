package com.petlis.util;

import java.time.Year;

/**
 * 报告编号生成工具类
 * 生成德国报告编号，格式：RAB{年}{订单ID}
 * 例如：RAB20251（2025年第1个订单）、RAB2025100（2025年第100个订单）
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
public class ReportNOGenerator {

    /**
     * 生成报告编号
     * 格式：RAB{4位年份}{订单ID}
     *
     * @param orderId 订单ID
     * @return 报告编号，例如 "RAB20251"
     */
    public static String generate(Integer orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }

        String year = String.valueOf(Year.now().getValue());
        return String.format("RAB%s%d", year, orderId);
    }

    /**
     * 验证报告编号格式
     *
     * @param reportNO 报告编号
     * @return 是否有效
     */
    public static boolean isValid(String reportNO) {
        if (reportNO == null || reportNO.trim().isEmpty()) {
            return false;
        }

        // 格式：RAB + 4位数字 + 订单ID
        return reportNO.matches("^RAB\\d{5,}$");
    }

    /**
     * 从报告编号中提取年份
     *
     * @param reportNO 报告编号
     * @return 年份，例如 "2025"
     */
    public static String extractYear(String reportNO) {
        if (!isValid(reportNO)) {
            throw new IllegalArgumentException("无效的报告编号格式");
        }

        // 提取RAB后的4位年份
        return reportNO.substring(3, 7);
    }

    /**
     * 从报告编号中提取订单ID
     *
     * @param reportNO 报告编号
     * @return 订单ID
     */
    public static Integer extractOrderId(String reportNO) {
        if (!isValid(reportNO)) {
            throw new IllegalArgumentException("无效的报告编号格式");
        }

        // 提取年份后的订单ID
        String orderIdStr = reportNO.substring(7);
        return Integer.parseInt(orderIdStr);
    }
}
