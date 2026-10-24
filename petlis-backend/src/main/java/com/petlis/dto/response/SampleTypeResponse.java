package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 样本类型响应DTO
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "样本类型响应")
public class SampleTypeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "样本类型ID")
    private Integer infoSampleTypeId;

    @Schema(description = "采样类型名称")
    private String sampleName;

    @Schema(description = "采样类型名称(英文)")
    private String sampleNameEn;

    @Schema(description = "启用状态(0=禁用,1=启用)")
    private Integer isActive;

    @Schema(description = "创建时间(Unix时间戳)")
    private Integer createtime;

    @Schema(description = "更新时间(Unix时间戳)")
    private Integer updatetime;
}
