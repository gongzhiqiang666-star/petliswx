package com.petlis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 查询检测申请请求DTO
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Schema(description = "查询检测申请请求")
public class OrderQueryRequest {
    private String keyword;

    @Schema(description = "页码(从0开始)", example = "0")
    private Integer page = 0;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    @Schema(description = "样本状态(-2=草稿,0=待审核,1=审核未通过,2=等待报告,3=已完成,4=已取消)")
    private Integer status;

    @Schema(description = "开始日期")
    private Date startDate;

    @Schema(description = "结束日期")
    private Date endDate;

    @Schema(description = "宠物姓名(模糊查询)")
    private String animalName;

    @Schema(description = "用户编号")
    private String userNO;

    @Schema(description = "芯片号")
    private String chipNO;

    @Schema(description = "医院ID（由Controller从token中自动填充）")
    private Integer hospitalId;
}
