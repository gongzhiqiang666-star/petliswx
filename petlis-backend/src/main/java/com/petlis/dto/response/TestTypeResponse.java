package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 检测项目响应DTO
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "检测项目响应")
public class TestTypeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "检测项目ID")
    private Integer infoTestTypeId;

    @Schema(description = "检测项目名称")
    private String testName;

    @Schema(description = "检测编号")
    private String testNO;

    @Schema(description = "价格")
    private Double price;

    @Schema(description = "中国到德国费用")
    private Double ctoG;

    @Schema(description = "德国到中国费用")
    private Double gtoC;

    @Schema(description = "当前折扣")
    private Double discountReal;

    @Schema(description = "待审折扣")
    private Double discountNext;

    @Schema(description = "创建日期")
    private Date createDt;

    @Schema(description = "启用状态(0=禁用,1=启用)")
    private Integer isActive;

    @Schema(description = "类型(0=other,1=细胞学&组织病理,2=过敏原,3=微生物,4=综合)")
    private Integer type;

    @Schema(description = "更新时间(Unix时间戳)")
    private Integer updatetime;

    @Schema(description = "创建时间(Unix时间戳)")
    private Integer createtime;
}
