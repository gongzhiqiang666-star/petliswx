package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 检测项目价格响应DTO
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "检测项目价格响应")
public class TestTypePriceResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "价格ID")
    private Long iid;

    @Schema(description = "检测项目ID")
    private Long infoTestTypeid;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "启用状态(0=禁用,1=启用)")
    private Integer isActive;

    @Schema(description = "创建时间(Unix时间戳)")
    private Integer createtime;

    @Schema(description = "更新时间(Unix时间戳)")
    private Integer updatetime;
}
