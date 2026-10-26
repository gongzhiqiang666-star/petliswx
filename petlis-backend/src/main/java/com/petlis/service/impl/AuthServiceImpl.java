package com.petlis.service.impl;

import com.petlis.common.dto.request.WechatLoginRequest;
import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.util.JwtUtil;
import com.petlis.common.util.WechatUtil;
import com.petlis.dto.response.UserInfoResponse;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.entity.business.User;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.UserRepository;
import com.petlis.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * 认证服务实现类
 *
 * 实现微信登录、token刷新、登出等功能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final InfoHospitalRepository hospitalRepository;
    private final JwtUtil jwtUtil;
    private final WechatUtil wechatUtil;
    private final SessionService sessionService;

    /**
     * 默认医院ID (新用户关联的默认医院)
     */


    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatLoginResponse wechatLogin(WechatLoginRequest request) {
        log.info("开始微信登录");

        // 1. 调用微信接口获取openid
        WechatUtil.WechatLoginResponse wechatResponse;
        try {
            wechatResponse = wechatUtil.code2Session(request.getCode());
            log.debug("微信凭证交换成功");
        } catch (Exception e) {
            log.error("微信登录失败: {}", e.getMessage(), e);
            throw new BusinessException("微信登录失败: " + e.getMessage());
        }

        String openid = wechatResponse.openid();

        User user = userRepository.findByOpenid(openid)
                .orElseThrow(() -> new BusinessException(428, "请先注册或登录已有医生账号并绑定微信"));
        WechatLoginResponse response = sessionService.issue(user.getId());
        user.setLogintime((int) Instant.now().getEpochSecond());
        userRepository.save(user);
        return response;
    }

    @Override
    public String refreshToken(String token) {
        if (!sessionService.valid(token)) throw new BusinessException(401, "会话已失效，请重新登录");
        return sessionService.issue(jwtUtil.getUserIdFromToken(token)).getToken();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String token) {
        if (!sessionService.valid(token)) throw new BusinessException(401, "会话已失效");
        sessionService.revoke(jwtUtil.getUserIdFromToken(token));
    }

}
