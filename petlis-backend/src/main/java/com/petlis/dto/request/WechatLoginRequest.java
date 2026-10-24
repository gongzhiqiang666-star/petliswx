package com.petlis.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求DTO
 *
 * @author PETLIS Team
 */
@Data
@Schema(description = "微信登录请求")
public class WechatLoginRequest {

    @Schema(description = "微信登录code", required = true, example = "071abc2w3defG4H5I6J7")
    @NotBlank(message = "登录code不能为空")
    private String code;
}
