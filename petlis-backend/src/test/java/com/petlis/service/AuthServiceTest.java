package com.petlis.service;

import com.petlis.common.dto.request.WechatLoginRequest;
import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.common.security.SecurityUtils;
import com.petlis.entity.business.User;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.UserRepository;
import com.petlis.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 认证服务测试类
 *
 * 测试微信登录、token刷新、登出等功能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InfoHospitalRepository hospitalRepository;

    @Mock
    private com.petlis.common.util.JwtUtil jwtUtil;

    @Mock
    private com.petlis.common.util.WechatUtil wechatUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private WechatLoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        loginRequest = new WechatLoginRequest();
        loginRequest.setCode("test_code_123");

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setOpenid("test_openid_123");
        mockUser.setHospitalId(1);
        mockUser.setUsername("test_openid_123");
        mockUser.setNickname("测试用户");
        mockUser.setStatus("normal");
    }

    @Test
    void testWechatLogin_ExistingUser() {
        // Given: 模拟微信返回openid
        when(wechatUtil.code2Session(anyString()))
                .thenReturn(new com.petlis.common.util.WechatUtil.WechatLoginResponse("test_openid_123", "session_key"));

        // Given: 模拟用户已存在
        when(userRepository.findByOpenid("test_openid_123"))
                .thenReturn(Optional.of(mockUser));

        // Given: 模拟生成token
        when(jwtUtil.generateToken(any(), anyString(), any(), any()))
                .thenReturn("mock_jwt_token");

        // When: 执行微信登录
        WechatLoginResponse response = authService.wechatLogin(loginRequest);

        // Then: 验证结果
        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals(1, response.getUserId());
        assertEquals(1, response.getHospitalId());
        assertEquals("test_openid_123", response.getOpenid());

        // 验证方法调用
        verify(wechatUtil).code2Session("test_code_123");
        verify(userRepository).findByOpenid("test_openid_123");
        verify(jwtUtil).generateToken(1, "test_openid_123", 1, null);
    }

    @Test
    void testWechatLogin_NewUser() {
        // Given: 模拟微信返回openid
        when(wechatUtil.code2Session(anyString()))
                .thenReturn(new com.petlis.common.util.WechatUtil.WechatLoginResponse("new_openid_456", "session_key"));

        // Given: 模拟用户不存在
        when(userRepository.findByOpenid("new_openid_456"))
                .thenReturn(Optional.empty());

        // Given: 模拟保存新用户
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(2);
                    return user;
                });

        // Given: 模拟生成token
        when(jwtUtil.generateToken(any(), anyString(), any(), any()))
                .thenReturn("mock_jwt_token");

        // When: 执行微信登录
        WechatLoginResponse response = authService.wechatLogin(loginRequest);

        // Then: 验证结果
        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals(2, response.getUserId());
        assertEquals(1, response.getHospitalId()); // 默认医院ID
        assertEquals("new_openid_456", response.getOpenid());

        // 验证新用户被创建
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRefreshToken_Success() {
        // Given: 模拟token验证通过
        when(jwtUtil.validateToken("old_token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("old_token")).thenReturn(1);
        when(jwtUtil.getOpenidFromToken("old_token")).thenReturn("test_openid");
        when(jwtUtil.getHospitalIdFromToken("old_token")).thenReturn(1);
        when(jwtUtil.getCityIdFromToken("old_token")).thenReturn(null);

        // Given: 模拟用户存在
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        // Given: 模拟生成新token
        when(jwtUtil.generateToken(1, "test_openid", 1, null))
                .thenReturn("new_token");

        // When: 刷新token
        String newToken = authService.refreshToken("old_token");

        // Then: 验证结果
        assertEquals("new_token", newToken);
        verify(jwtUtil).generateToken(1, "test_openid", 1, null);
    }

    @Test
    void testRefreshToken_InvalidToken() {
        // Given: 模拟token验证失败
        when(jwtUtil.validateToken("invalid_token")).thenReturn(false);

        // When & Then: 刷新token应该抛出异常
        assertThrows(com.petlis.common.exception.BusinessException.class, () -> {
            authService.refreshToken("invalid_token");
        });
    }

    @Test
    void testLogout_Success() {
        // Given: 模拟token验证通过
        when(jwtUtil.validateToken("valid_token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid_token")).thenReturn(1);

        // When: 执行登出
        authService.logout("valid_token");

        // Then: 验证方法被调用
        verify(jwtUtil).validateToken("valid_token");
        verify(jwtUtil).getUserIdFromToken("valid_token");
        // 注意: 当前实现只是记录日志,没有黑名单机制
    }
}
