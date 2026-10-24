package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 城市响应DTO
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "城市响应")
public class CityResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "城市ID")
    private Integer cityId;

    @Schema(description = "城市名称")
    private String cityName;

    @Schema(description = "区号")
    private Integer areaCode;

    @Schema(description = "级别")
    private String grade;

    @Schema(description = "邮编")
    private Integer zipCode;

    @Schema(description = "所属城市ID")
    private Integer belongCityCityId;

    @Schema(description = "启用状态(0=禁用,1=启用)")
    private Integer isActive;
}
