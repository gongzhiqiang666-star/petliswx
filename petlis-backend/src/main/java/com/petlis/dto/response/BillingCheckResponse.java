package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 账单检查响应
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "账单检查响应")
public class BillingCheckResponse {

    @Schema(description = "是否为月结客户")
    private Boolean isMonthlyCustomer;

    @Schema(description = "是否有未支付账单")
    private Boolean hasUnpaidBills;

    @Schema(description = "未支付金额（元）")
    private Double unpaidAmount;

    @Schema(description = "未支付订单数量")
    private Integer unpaidCount;

    @Schema(description = "账单月份（格式：yyyy-MM）")
    private String billMonth;

    @Schema(description = "是否允许查看PDF")
    private Boolean allowViewPdf;

    @Schema(description = "提示消息")
    private String message;
}
