package com.petlis.controller.api;

import com.petlis.common.annotation.OperationLog;
import com.petlis.common.dto.request.WechatLoginRequest;
import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.common.response.Result;
import com.petlis.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * 提供微信登录、token刷新、登出等API接口
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 微信登录
     *
     * 通过微信code换取openid,查询或创建用户,返回JWT token
     *
     * @param request 微信登录请求
     * @return 登录响应(包含token、用户ID、医院ID等)
     */
    @PostMapping("/wechat/login")
    @Operation(summary = "微信登录", description = "使用微信code进行登录,返回JWT token")
    @OperationLog(module = "认证管理", operation = "微信登录", description = "用户使用微信code登录系统")
    public Result<WechatLoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        log.info("收到微信登录请求");

        try {
            WechatLoginResponse response = authService.wechatLogin(request);
            log.info("微信登录成功, userId: {}, hospitalId: {}", response.getUserId(), response.getHospitalId());
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("微信登录失败: {}", e.getMessage(), e);
            return Result.fail("登录失败: " + e.getMessage());
        }
    }

    /**
     * 刷新token
     *
     * 使用当前token生成新的token
     *
     * @param token 当前有效的JWT token
     * @return 新的JWT token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用当前token刷新获取新的token")
    @OperationLog(module = "认证管理", operation = "刷新Token", description = "用户刷新访问令牌")
    public Result<String> refreshToken(@RequestHeader("Authorization") String token) {
        log.info("收到刷新token请求");

        try {
            // 提取Bearer token
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String newToken = authService.refreshToken(token);
            log.info("刷新token成功");
            return Result.success("刷新成功", newToken);
        } catch (Exception e) {
            log.error("刷新token失败: {}", e.getMessage(), e);
            return Result.fail("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 登出
     *
     * 将token加入黑名单(可选实现)
     *
     * @param token JWT token
     * @return 登出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "登出", description = "用户登出系统")
    @OperationLog(module = "认证管理", operation = "登出", description = "用户登出系统")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        log.info("收到登出请求");

        try {
            // 提取Bearer token
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            authService.logout(token);
            log.info("登出成功");
            return Result.success("登出成功");
        } catch (Exception e) {
            log.error("登出失败: {}", e.getMessage(), e);
            return Result.fail("登出失败: " + e.getMessage());
        }
    }
}
