package com.petlis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新请求DTO
 *
 * 用于更新用户的昵称、头像、手机号等信息
 * 不允许修改openid、hospitalId等核心字段
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "用户信息更新请求")
public class UserUpdateRequest {

    @Schema(description = "昵称", example = "张三", maxLength = 50)
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg", maxLength = 255)
    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatarUrl;

    @Schema(description = "手机号", example = "13800138000", pattern = "^1[3-9]\\d{9}$")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @Schema(description = "邮箱", example = "user@example.com", maxLength = 100)
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "性别 (0=未知, 1=男, 2=女)", example = "1")
    private Integer gender;

    @Schema(description = "生日", example = "1990-01-01")
    private String birthday;

    @Schema(description = "格言", example = "热爱生活", maxLength = 100)
    @Size(max = 100, message = "格言长度不能超过100个字符")
    private String bio;
}
