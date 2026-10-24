package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 品种响应DTO
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "品种响应")
public class VarietyResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "品种ID")
    private Long iid;

    @Schema(description = "品种名称(中文)")
    private String varietyCn;

    @Schema(description = "品种名称(英文)")
    private String varietyEn;

    @Schema(description = "创建时间(Unix时间戳)")
    private Integer createtime;

    @Schema(description = "更新时间(Unix时间戳)")
    private Integer updatetime;
}
