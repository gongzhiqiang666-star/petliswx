package com.petlis.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.petlis.common.validation.AgeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 创建检测申请请求DTO
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "创建检测申请请求")
public class OrderCreateRequest {
    private Long draftId;
    private Integer draftVersion;
    private Integer laboratoryId;
    private Long userCouponId;

    @Schema(description = "城市ID（已废弃，从token中获取）")
    private Integer cityId;  // 不再需要前端传递，从token中获取

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

    @Schema(description = "体重(可选，格式建议: 15kg)")
    private String animalWeight;

    @Schema(description = "芯片号(狂犬检测必填)")
    private String chipNO;

    @NotBlank(message = "年龄不能为空")
    @AgeFormat(message = "年龄格式错误，正确格式如：3Y7M、5M、3Y")
    @Schema(description = "年龄(格式: Y/M，如3Y7M、5M、3Y)", required = true)
    private String age;

    @Schema(description = "动物生日(狂犬检测必填)")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
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
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date collectDt;

    @Schema(description = "收到样本日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date receiveSampleDt;

    @Schema(description = "送检医师姓名")
    private String doctorName;

    @Schema(description = "市场活动或合作公司")
    private String marketingName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "检测明细列表", required = true)
    private List<DiagnosisDetailItem> diagnosisDetails;

    /**
     * 检测明细项DTO
     */
    @Data
    public static class DiagnosisDetailItem {
        @Schema(description = "检测项目ID", required = true)
        private Integer infoTestTypeId;
    }
}
