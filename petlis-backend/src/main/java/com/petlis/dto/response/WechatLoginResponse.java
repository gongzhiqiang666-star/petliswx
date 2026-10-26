package com.petlis.common.dto.response;

import com.petlis.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信登录响应DTO
 *
 * @author PETLIS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信登录响应")
public class WechatLoginResponse {

    @Schema(description = "访问令牌")
    private String token;

    private boolean reviewer;

    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "医院ID")
    private Integer hospitalId;

    @Schema(description = "医院名称")
    private String hospitalName;

    @Schema(description = "医院信息")
    private UserInfoResponse.HospitalInfo hospital;

    @Schema(description = "微信openid")
    private String openid;
}
