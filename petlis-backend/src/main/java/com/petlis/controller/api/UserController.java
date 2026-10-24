package com.petlis.controller.api;

import com.petlis.common.annotation.OperationLog;
import com.petlis.common.response.Result;
import com.petlis.dto.request.UserUpdateRequest;
import com.petlis.dto.response.UserInfoResponse;
import com.petlis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户控制器
 *
 * 提供用户信息查询、更新、头像上传等API接口
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息相关接口")
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     *
     * 返回当前登录用户的详细信息,包括关联的医院信息
     *
     * @return 用户信息响应
     */
    @GetMapping("/profile")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    @OperationLog(module = "用户管理", operation = "查询用户信息", description = "查询当前用户详细信息")
    public Result<UserInfoResponse> getUserProfile() {
        log.info("收到获取用户信息请求");

        try {
            UserInfoResponse response = userService.getUserProfile();
            log.info("获取用户信息成功, userId: {}, nickname: {}", response.getId(), response.getNickname());
            return Result.success("获取成功", response);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage(), e);
            return Result.fail("获取失败: " + e.getMessage());
        }
    }

    /**
     * 更新当前用户信息
     *
     * 更新当前登录用户的昵称、头像、手机号等信息
     * 不允许修改openid、hospitalId等核心字段
     *
     * @param request 用户更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/profile")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的信息")
    @OperationLog(module = "用户管理", operation = "更新用户信息", description = "更新用户昵称、头像、手机号等")
    public Result<UserInfoResponse> updateUserProfile(@Valid @RequestBody UserUpdateRequest request) {
        log.info("收到更新用户信息请求");

        try {
            UserInfoResponse response = userService.updateUserProfile(request);
            log.info("更新用户信息成功, userId: {}", response.getId());
            return Result.success("更新成功", response);
        } catch (Exception e) {
            log.error("更新用户信息失败: {}", e.getMessage(), e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 上传用户头像
     *
     * 上传头像图片文件,验证文件类型和大小
     * 支持的格式: jpg, jpeg, png
     * 最大文件大小: 2MB
     *
     * @param file 头像图片文件
     * @return 头像访问URL
     */
    @PostMapping("/avatar")
    @Operation(summary = "上传头像", description = "上传用户头像图片")
    @OperationLog(module = "用户管理", operation = "上传头像", description = "用户上传头像图片")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        log.info("收到上传头像请求, 文件名: {}, 文件大小: {}", file.getOriginalFilename(), file.getSize());

        try {
            String avatarUrl = userService.uploadAvatar(file);
            log.info("上传头像成功, URL: {}", avatarUrl);
            return Result.success("上传成功", avatarUrl);
        } catch (Exception e) {
            log.error("上传头像失败: {}", e.getMessage(), e);
            return Result.fail("上传失败: " + e.getMessage());
        }
    }
}
