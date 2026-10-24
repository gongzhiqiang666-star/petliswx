package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 动物物种响应DTO
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "动物物种响应")
public class AnimalResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "物种ID")
    private Integer infoAnimalId;

    @Schema(description = "物种名称(中文)")
    private String animalName;

    @Schema(description = "物种名称(英文)")
    private String animalNameEn;

    @Schema(description = "启用状态(0=禁用,1=启用)")
    private Integer isActive;

    @Schema(description = "创建时间(Unix时间戳)")
    private Integer createtime;

    @Schema(description = "更新时间(Unix时间戳)")
    private Integer updatetime;
}
