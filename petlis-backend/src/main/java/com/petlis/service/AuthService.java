package com.petlis.service;

import com.petlis.common.dto.request.WechatLoginRequest;
import com.petlis.common.dto.response.WechatLoginResponse;

/**
 * 认证服务接口
 *
 * 提供微信登录、token刷新、登出等功能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * 微信登录
     *
     * 通过微信code换取openid,查询或创建用户,生成JWT token
     *
     * @param request 微信登录请求(包含code)
     * @return 登录响应(包含token、用户ID、医院ID等信息)
     */
    WechatLoginResponse wechatLogin(WechatLoginRequest request);

    /**
     * 刷新token
     *
     * 使用当前token生成新的token
     *
     * @param token 当前有效的JWT token
     * @return 新的JWT token
     */
    String refreshToken(String token);

    /**
     * 登出
     *
     * 将token加入黑名单(可选实现)
     *
     * @param token JWT token
     */
    void logout(String token);
}
