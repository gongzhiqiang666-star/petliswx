package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 检测申请详情响应DTO(包含主表和明细表)
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "检测申请详情响应")
public class OrderDetailResponse {

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

    @Schema(description = "寄出样本日期")
    private Date sendSampleDt;

    @Schema(description = "报告发出日期")
    private Date sendReportDt;

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

    @Schema(description = "检测项目明细列表")
    private List<OrderItemResponse> items;

    /**
     * 检测项目明细响应
     */
    @Data
    @Schema(description = "检测项目明细")
    public static class OrderItemResponse {

        @Schema(description = "明细ID")
        private Integer itemId;

        @Schema(description = "生成编号")
        private String genterNO;

        @Schema(description = "检测项目ID")
        private Integer infoTestTypeId;

        @Schema(description = "检测项目名称")
        private String testTypeName;

        @Schema(description = "检测价格")
        private BigDecimal testPrice;

        @Schema(description = "原始价格")
        private BigDecimal orginalPrice;

        @Schema(description = "原始折扣")
        private BigDecimal orginalDiscount;

        @Schema(description = "实际费用")
        private Double realFee;

        @Schema(description = "详细备注")
        private String detailRemark;

        @Schema(description = "支付状态(0=未结,1=现金,2=转账,3=网银已支付,4=忽略)")
        private Integer payStatus2;

        @Schema(description = "诊断状态(0=待检测)")
        private Integer diagnosisStatus;

        @Schema(description = "是否激活(0=禁用,1=启用)")
        private Integer isActive;

        @Schema(description = "创建时间")
        private Date createDt;

        @Schema(description = "PDF访问URL(相对路径)")
        private String pdfHttpUrl;

        @Schema(description = "PDF完整URL(直接访问，无权限验证)")
        private String pdfFullUrl;

        @Schema(description = "PDF安全访问URL(通过API访问，带权限验证)")
        private String securePdfUrl;

        @Schema(description = "是否已发送报告(PDF已生成) 0=未生成,1=已生成")
        private Integer isSendMail;

        @Schema(description = "发送邮件时间(报告生成时间)")
        private Date sendMailDt;
    }
}
