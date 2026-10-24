package com.petlis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付请求
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "创建支付请求")
public class PaymentCreateRequest {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID")
    private Integer orderId;

    @Schema(description = "支付金额（可选，默认使用订单金额）")
    private BigDecimal amount;

    @NotBlank(message = "微信用户openid不能为空")
    @Schema(description = "微信用户openid")
    private String openid;
}
