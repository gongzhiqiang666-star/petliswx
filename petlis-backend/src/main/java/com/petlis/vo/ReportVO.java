package com.petlis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 检测报告视图对象
 * 包含完整的报告信息
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "检测报告视图对象")
public class ReportVO {

    /**
     * 报告ID (检测明细ID)
     */
    @Schema(description = "报告ID")
    private Integer reportId;

    /**
     * 申请单ID
     */
    @Schema(description = "申请单ID")
    private Integer orderId;

    /**
     * 生成编号
     */
    @Schema(description = "生成编号")
    private String genterNO;

    /**
     * 宠物信息
     */
    @Schema(description = "宠物姓名")
    private String animalName;

    @Schema(description = "宠物品种")
    private String animalType;

    @Schema(description = "宠物性别")
    private String dictSexName;

    @Schema(description = "宠物年龄")
    private String age;

    @Schema(description = "宠物体重")
    private String animalWeight;

    @Schema(description = "芯片号")
    private String chipNO;

    /**
     * 检测项目信息
     */
    @Schema(description = "检测项目ID")
    private Integer infoTestTypeId;

    @Schema(description = "检测项目名称")
    private String testTypeName;

    @Schema(description = "检测价格")
    private BigDecimal testPrice;

    /**
     * 检测状态信息
     */
    @Schema(description = "检测状态 (0=检测中, 其他=已完成)")
    private Integer diagnosisStatus;

    @Schema(description = "检测状态描述")
    private String statusText;

    /**
     * 报告内容
     */
    @Schema(description = "报告结果 (HTML格式)")
    private String reportResult;

    @Schema(description = "邮件报告内容")
    private String mailReport;

    /**
     * PDF报告
     */
    @Schema(description = "PDF报告URL")
    private String pdfHttpUrl;

    /**
     * 时间信息
     */
    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "采样日期")
    private Date collectDt;

    @Schema(description = "收到样本日期")
    private Date receiveSampleDt;

    @Schema(description = "报告发出日期")
    private Date sendReportDt;

    /**
     * 申请单信息
     */
    @Schema(description = "报告编号")
    private String reportNO;

    @Schema(description = "畜主姓名")
    private String masterName;

    @Schema(description = "送检医师")
    private String doctorName;

    @Schema(description = "医院ID")
    private Integer hospitalId;

    @Schema(description = "医院名称")
    private String hospitalName;

    @Schema(description = "样本类型名称")
    private String sampleTypeName;

    @Schema(description = "实验室名称")
    private String laboratoryName;

    @Schema(description = "订单是否已支付")
    private Boolean paid;

    @Schema(description = "结算方式")
    private String settlementMode;

    @Schema(description = "现结订单未支付时报告内容受限")
    private Boolean paymentRestricted = false;
}
