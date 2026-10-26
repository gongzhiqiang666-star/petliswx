package com.petlis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 检测报告查询请求DTO
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "检测报告查询请求")
public class ReportQueryRequest {

    @Schema(description = "宠物名称或检测项目关键词")
    private String keyword;

    /**
     * 宠物ID
     */
    @Schema(description = "宠物ID")
    private Integer petId;

    /**
     * 检测状态 (0=检测中, 其他=已完成)
     */
    @Schema(description = "检测状态 (0=检测中, 其他=已完成)")
    private Integer status;

    /**
     * 开始日期 (格式: yyyy-MM-dd)
     */
    @Schema(description = "开始日期", example = "2024-01-01")
    private String startDate;

    /**
     * 结束日期 (格式: yyyy-MM-dd)
     */
    @Schema(description = "结束日期", example = "2024-12-31")
    private String endDate;

    /**
     * 页码 (从0开始)
     */
    @Schema(description = "页码", example = "0")
    @Min(value = 0, message = "页码不能小于0")
    private Integer pageNum = 0;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "10")
    @Min(value = 1, message = "每页大小不能小于1")
    private Integer pageSize = 10;
}
