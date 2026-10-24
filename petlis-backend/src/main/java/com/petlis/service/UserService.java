package com.petlis.service;

import com.petlis.dto.request.UserUpdateRequest;
import com.petlis.dto.response.UserInfoResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口
 *
 * 提供用户信息查询、更新、头像上传等功能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 获取当前用户信息
     *
     * 从SecurityContext中获取当前登录用户的ID
     * 查询用户详细信息以及关联的医院信息
     *
     * @return 用户信息响应(包含医院信息)
     */
    UserInfoResponse getUserProfile();

    /**
     * 更新当前用户信息
     *
     * 从SecurityContext中获取当前登录用户的ID
     * 更新用户的昵称、头像、手机号等可修改字段
     * 不允许修改openid、hospitalId等核心字段
     * 自动更新updatetime时间戳
     *
     * @param request 用户更新请求
     * @return 更新后的用户信息
     */
    UserInfoResponse updateUserProfile(UserUpdateRequest request);

    /**
     * 上传用户头像
     *
     * 验证文件类型和大小
     * 生成唯一文件名并保存到文件系统
     * 返回可访问的头像URL
     *
     * @param file 头像图片文件
     * @return 头像访问URL
     */
    String uploadAvatar(MultipartFile file);
}
