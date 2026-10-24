package com.petlis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单检测项添加请求DTO
 *
 * 用于向检测申请中添加检测项目
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "订单检测项添加请求")
public class OrderItemRequest {

    @NotNull(message = "检测项目ID不能为空")
    @Schema(description = "检测项目ID", required = true, example = "1")
    private Integer testTypeId;

    @NotNull(message = "城市ID不能为空")
    @Schema(description = "城市ID（默认为0表示基础价格）", required = true, example = "0")
    private Integer cityId;
}
