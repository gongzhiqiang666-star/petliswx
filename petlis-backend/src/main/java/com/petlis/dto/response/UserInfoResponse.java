package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应DTO
 *
 * 包含用户的所有基本信息以及关联的医院信息
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息响应")
public class UserInfoResponse {

    @Schema(description = "用户ID")
    private Integer id;

    @Schema(description = "组别ID")
    private Integer groupId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "电子邮箱")
    private String email;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "等级")
    private Integer level;

    @Schema(description = "性别 (0=未知, 1=男, 2=女)")
    private Integer gender;

    @Schema(description = "生日")
    private String birthday;

    @Schema(description = "格言")
    private String bio;

    @Schema(description = "余额")
    private String money;

    @Schema(description = "积分")
    private Integer score;

    @Schema(description = "连续登录天数")
    private Integer successions;

    @Schema(description = "最大连续登录天数")
    private Integer maxsuccessions;

    @Schema(description = "上次登录时间(Unix时间戳)")
    private Integer prevtime;

    @Schema(description = "登录时间(Unix时间戳)")
    private Integer logintime;

    @Schema(description = "创建时间(Unix时间戳)")
    private Integer createtime;

    @Schema(description = "更新时间(Unix时间戳)")
    private Integer updatetime;

    @Schema(description = "微信openid")
    private String openid;

    @Schema(description = "医院ID")
    private Integer hospitalId;

    @Schema(description = "医院信息")
    private HospitalInfo hospital;

    /**
     * 医院信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "医院信息")
    public static class HospitalInfo {

        @Schema(description = "医院ID")
        private Integer hospitalId;

        @Schema(description = "医院名称")
        private String hospitalName;

        @Schema(description = "医院地址")
        private String place;

        @Schema(description = "联系电话")
        private String tel;

        @Schema(description = "城市ID")
        private Integer cityId;
    }
}
