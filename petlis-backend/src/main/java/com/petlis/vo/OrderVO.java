package com.petlis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 检测申请视图对象
 * 用于业务层和数据传输层之间的数据转换
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "检测申请视图对象")
public class OrderVO {

    @Schema(description = "申请单ID")
    private Integer orderId;

    @Schema(description = "用户编号(人工编号)")
    private String userNO;

    @Schema(description = "城市ID")
    private Integer cityId;

    @Schema(description = "医院ID")
    private Integer hospitalId;

    @Schema(description = "动物物种ID")
    private Integer infoAnimalId;

    @Schema(description = "采样类型ID")
    private Integer infoSampleTypeId;

    @Schema(description = "宠物姓名")
    private String animalName;

    @Schema(description = "宠物姓名(英文)")
    private String animalNameEn;

    @Schema(description = "动物品种ID")
    private Integer animalTypeId;

    @Schema(description = "品种")
    private String animalType;

    @Schema(description = "品种(英文)")
    private String animalTypeEn;

    @Schema(description = "体重")
    private String animalWeight;

    @Schema(description = "芯片号")
    private String chipNO;

    @Schema(description = "年龄")
    private String age;

    @Schema(description = "动物生日")
    private Date birthday;

    @Schema(description = "性别ID")
    private Integer dictSexId;

    @Schema(description = "采样部位")
    private String sampleArea;

    @Schema(description = "畜主姓名")
    private String masterName;

    @Schema(description = "畜主姓名(英文)")
    private String masterNameEn;

    @Schema(description = "采样日期")
    private Date collectDt;

    @Schema(description = "收到样本日期")
    private Date receiveSampleDt;

    @Schema(description = "检测总费用")
    private Double testFee;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "样本状态")
    private Integer sampleStatus;

    @Schema(description = "样本状态描述")
    private String sampleStatusDesc;

    @Schema(description = "创建时间")
    private Date createDt;

    @Schema(description = "送检医师姓名")
    private String doctorName;

    @Schema(description = "市场活动或合作公司")
    private String marketingName;

    @Schema(description = "检测项目数量")
    private Integer itemCount;

    @Schema(description = "检测总价格")
    private BigDecimal totalPrice;
}
