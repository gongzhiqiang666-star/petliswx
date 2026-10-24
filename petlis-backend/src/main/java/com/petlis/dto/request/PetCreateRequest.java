package com.petlis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建宠物档案请求DTO
 *
 * @author PETLIS Team
 */
@Data
@Schema(description = "创建宠物档案请求")
public class PetCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "宠物中文名", required = true, example = "旺财")
    @NotBlank(message = "宠物中文名不能为空")
    private String animalName;

    @Schema(description = "宠物英文名", example = "Lucky")
    private String animalNameEn;

    @Schema(description = "物种ID", required = true, example = "1")
    @NotNull(message = "物种ID不能为空")
    private Integer infoAnimalId;

    @Schema(description = "品种ID", example = "10")
    private Integer infoVarietyId;

    @Schema(description = "性别ID", required = true, example = "1")
    @NotNull(message = "性别ID不能为空")
    private Integer dictSexId;

    @Schema(description = "生日(格式: yyyy-MM-dd)", example = "2020-01-15")
    private String birthday;

    @Schema(description = "芯片号", example = "928000000000001")
    private String chipNo;

    @Schema(description = "畜主中文名", required = true, example = "张三")
    @NotBlank(message = "畜主中文名不能为空")
    private String ownerName;

    @Schema(description = "畜主英文名", example = "Zhang San")
    private String ownerNameEn;
}
