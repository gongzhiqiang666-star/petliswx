package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 性别响应DTO
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "性别响应")
public class SexResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "性别ID")
    private Integer dictSexId;

    @Schema(description = "性别名称")
    private String sexName;

    @Schema(description = "性别名称(英文)")
    private String sexNameEn;

    @Schema(description = "启用状态(0=禁用,1=启用)")
    private Integer isActive;

    @Schema(description = "创建时间(Unix时间戳)")
    private Integer createtime;

    @Schema(description = "更新时间(Unix时间戳)")
    private Integer updatetime;
}
