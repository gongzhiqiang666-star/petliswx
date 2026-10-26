package com.petlis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付响应DTO
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "支付响应")
public class PaymentResponse {
    private boolean mock;

    @Schema(description = "支付订单号")
    private String paymentNo;

    @Schema(description = "业务订单ID")
    private Integer orderId;

    @Schema(description = "支付金额")
    private String amount;

    @Schema(description = "微信预支付ID")
    private String prepayId;

    @Schema(description = "时间戳")
    private String timeStamp;

    @Schema(description = "随机字符串")
    private String nonceStr;

    @Schema(description = "签名类型")
    private String signType;

    @Schema(description = "支付类型（小程序支付固定为prepay_id=xxx）")
    @JsonProperty("package")
    private String packageValue;

    @Schema(description = "支付签名")
    private String paySign;

    @Schema(description = "支付状态")
    private Integer status;

    @Schema(description = "支付状态描述")
    private String statusDesc;
}
