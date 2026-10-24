package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单创建响应
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "订单创建响应")
public class OrderCreateResponse {

    @Schema(description = "订单ID")
    private Integer orderId;

    @Schema(description = "用户编号")
    private String userNO;

    @Schema(description = "订单金额")
    private Double totalFee;

    @Schema(description = "是否需要支付 (false=月结客户, true=需要立即支付)")
    private Boolean needPayment;

    @Schema(description = "支付类型描述")
    private String payTypeDesc;

    @Schema(description = "支付订单号（如果需要支付）")
    private String paymentNo;
}
