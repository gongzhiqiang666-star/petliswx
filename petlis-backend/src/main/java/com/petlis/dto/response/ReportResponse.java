package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 检测报告响应DTO
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "检测报告响应")
public class ReportResponse {

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
     * 宠物姓名
     */
    @Schema(description = "宠物姓名")
    private String animalName;

    /**
     * 宠物品种
     */
    @Schema(description = "宠物品种")
    private String animalType;

    /**
     * 检测项目名称
     */
    @Schema(description = "检测项目名称")
    private String testTypeName;

    /**
     * 检测状态 (0=检测中, 其他=已完成)
     */
    @Schema(description = "检测状态 (0=检测中, 其他=已完成)")
    private Integer diagnosisStatus;

    /**
     * 检测状态描述
     */
    @Schema(description = "检测状态描述")
    private String statusText;

    /**
     * 检测价格
     */
    @Schema(description = "检测价格")
    private BigDecimal testPrice;

    /**
     * PDF报告URL（相对路径）
     */
    @Schema(description = "PDF报告URL（相对路径）")
    private String pdfHttpUrl;

    /**
     * PDF完整URL（直接访问，无权限验证）
     */
    @Schema(description = "PDF完整URL（直接访问，无权限验证）")
    private String pdfFullUrl;

    /**
     * PDF安全访问URL（通过API访问，带权限验证）
     */
    @Schema(description = "PDF安全访问URL（通过API访问，带权限验证）")
    private String securePdfUrl;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 采样日期
     */
    @Schema(description = "采样日期")
    private Date collectDt;

    /**
     * 接收样本日期
     */
    @Schema(description = "接收样本日期")
    private Date receiveSampleDt;

    /**
     * 发送样本日期
     */
    @Schema(description = "发送样本日期")
    private Date sendSampleDt;

    /**
     * 送检医师姓名
     */
    @Schema(description = "送检医师姓名")
    private String doctorName;

    /**
     * 报告创建日期
     */
    @Schema(description = "报告创建日期")
    private Date createDt;

    /**
     * 报告发送日期
     */
    @Schema(description = "报告发送日期")
    private Date sendMailDt;

    /**
     * 报告编号
     */
    @Schema(description = "报告编号")
    private String reportNO;
}
