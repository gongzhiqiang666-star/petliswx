package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 医院响应DTO
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "医院响应")
public class HospitalResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "医院ID")
    private Integer infoHospitalId;

    @Schema(description = "医院名称")
    private String hospitalName;

    @Schema(description = "医院名称(英文)")
    private String hospitalNameEn;

    @Schema(description = "发票抬头")
    private String invoice;

    @Schema(description = "城市ID")
    private Integer cityId;

    @Schema(description = "医院地址")
    private String place;

    @Schema(description = "地址(备用字段)")
    private String adress;

    @Schema(description = "电话")
    private String tel;

    @Schema(description = "传真")
    private String fax;

    @Schema(description = "联系方式ID")
    private Integer infoHospitalContactId;

    @Schema(description = "创建日期")
    private Date createDt;

    @Schema(description = "启用状态(0=禁用,1=启用)")
    private Integer isActive;

    @Schema(description = "登录名")
    private String loginName;

    @Schema(description = "钱包金额")
    private BigDecimal amount;

    @Schema(description = "支付类型")
    private Integer payType;
}
