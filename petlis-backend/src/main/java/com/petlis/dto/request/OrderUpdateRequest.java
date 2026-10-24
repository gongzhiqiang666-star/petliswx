package com.petlis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 更新检测申请请求DTO
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "更新检测申请请求")
public class OrderUpdateRequest {

    @NotNull(message = "城市ID不能为空")
    @Schema(description = "城市ID", required = true)
    private Integer cityId;

    @NotNull(message = "动物物种ID不能为空")
    @Schema(description = "动物物种ID", required = true)
    private Integer infoAnimalId;

    @NotNull(message = "采样类型ID不能为空")
    @Schema(description = "采样类型ID", required = true)
    private Integer infoSampleTypeId;

    @NotBlank(message = "宠物姓名不能为空")
    @Schema(description = "宠物姓名", required = true)
    private String animalName;

    @NotBlank(message = "宠物姓名(英文)不能为空")
    @Schema(description = "宠物姓名(英文)", required = true)
    private String animalNameEn;

    @NotNull(message = "动物品种ID不能为空")
    @Schema(description = "动物品种ID", required = true)
    private Integer animalTypeId;

    @NotBlank(message = "品种不能为空")
    @Schema(description = "品种", required = true)
    private String animalType;

    @NotBlank(message = "品种(英文)不能为空")
    @Schema(description = "品种(英文)", required = true)
    private String animalTypeEn;

    @NotBlank(message = "体重不能为空")
    @Schema(description = "体重", required = true)
    private String animalWeight;

    @Schema(description = "芯片号(狂犬检测必填)")
    private String chipNO;

    @NotBlank(message = "年龄不能为空")
    @Schema(description = "年龄", required = true)
    private String age;

    @Schema(description = "动物生日(狂犬检测必填)")
    private Date birthday;

    @NotNull(message = "性别ID不能为空")
    @Schema(description = "性别ID", required = true)
    private Integer dictSexId;

    @NotBlank(message = "采样部位不能为空")
    @Schema(description = "采样部位", required = true)
    private String sampleArea;

    @NotBlank(message = "畜主姓名不能为空")
    @Schema(description = "畜主姓名", required = true)
    private String masterName;

    @NotBlank(message = "畜主姓名(英文)不能为空")
    @Schema(description = "畜主姓名(英文)", required = true)
    private String masterNameEn;

    @Schema(description = "采样日期")
    private Date collectDt;

    @Schema(description = "收到样本日期")
    private Date receiveSampleDt;

    @Schema(description = "送检医师姓名")
    private String doctorName;

    @Schema(description = "市场活动或合作公司")
    private String marketingName;

    @NotBlank(message = "备注不能为空")
    @Schema(description = "备注", required = true)
    private String remark;
}
