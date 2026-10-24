package com.petlis.service;

import com.petlis.dto.response.BillingCheckResponse;

/**
 * 账单服务接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
public interface BillingService {

    /**
     * 检查月结客户是否有未支付账单
     *
     * @param hospitalId 医院ID
     * @return 账单检查结果
     */
    BillingCheckResponse checkMonthlyBilling(Integer hospitalId);

    /**
     * 获取当前账单月份
     * 根据当前日期计算需要结清的账单月份
     *
     * @return 账单月份（格式：yyyy-MM）
     */
    String getCurrentBillMonth();
}
