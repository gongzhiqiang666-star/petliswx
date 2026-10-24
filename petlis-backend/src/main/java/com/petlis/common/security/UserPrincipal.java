package com.petlis.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户主体信息
 *
 * 用于在Spring Security的SecurityContext中存储用户认证信息
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 微信openid
     */
    private String openid;

    /**
     * 医院ID
     */
    private Integer hospitalId;

    /**
     * 城市ID
     */
    private Integer cityId;

    /**
     * 获取用户ID (用于Spring Security的principal)
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * 获取医院ID
     */
    public Integer getHospitalId() {
        return hospitalId;
    }

    /**
     * 获取城市ID
     */
    public Integer getCityId() {
        return cityId;
    }

    /**
     * 获取openid
     */
    public String getOpenid() {
        return openid;
    }
}
