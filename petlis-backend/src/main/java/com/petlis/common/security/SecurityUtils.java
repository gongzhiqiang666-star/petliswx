package com.petlis.common.security;

import com.petlis.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 *
 * 用于从SecurityContext中获取当前用户信息
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户主体信息
     *
     * @return 用户主体信息
     * @throws BusinessException 如果用户未登录
     */
    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("用户未登录");
        }

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }

        throw new BusinessException("用户信息异常");
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     * @throws BusinessException 如果用户未登录
     */
    public static Integer getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * 获取当前用户openid
     *
     * @return 微信openid
     * @throws BusinessException 如果用户未登录
     */
    public static String getCurrentOpenid() {
        return getCurrentUser().getOpenid();
    }

    /**
     * 获取当前医院ID
     *
     * @return 医院ID
     * @throws BusinessException 如果用户未登录
     */
    public static Integer getCurrentHospitalId() {
        return getCurrentUser().getHospitalId();
    }

    /**
     * 获取当前城市ID
     *
     * @return 城市ID
     * @throws BusinessException 如果用户未登录
     */
    public static Integer getCurrentCityId() {
        return getCurrentUser().getCityId();
    }

    /**
     * 检查用户是否已登录
     *
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal;
    }
}
