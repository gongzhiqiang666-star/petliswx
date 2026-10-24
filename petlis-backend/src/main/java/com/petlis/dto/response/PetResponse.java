package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宠物档案响应DTO
 *
 * @author PETLIS Team
 */
@Data
@Schema(description = "宠物档案响应")
public class PetResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "宠物ID")
    private Integer id;

    @Schema(description = "宠物中文名")
    private String animalName;

    @Schema(description = "宠物英文名")
    private String animalNameEn;

    @Schema(description = "物种ID")
    private Integer infoAnimalId;

    @Schema(description = "品种ID")
    private Integer infoVarietyId;

    @Schema(description = "性别ID")
    private Integer dictSexId;

    @Schema(description = "生日(格式: yyyy-MM-dd)")
    private String birthday;

    @Schema(description = "芯片号")
    private String chipNo;

    @Schema(description = "畜主中文名")
    private String ownerName;

    @Schema(description = "畜主英文名")
    private String ownerNameEn;

    @Schema(description = "所属医院ID")
    private Integer infoHospitalId;

    @Schema(description = "启用状态(0=禁用,1=启用)")
    private Integer isActive;

    @Schema(description = "创建时间")
    private Integer createtime;

    @Schema(description = "更新时间")
    private Integer updatetime;
}
