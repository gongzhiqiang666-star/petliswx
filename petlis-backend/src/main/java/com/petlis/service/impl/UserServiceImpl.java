package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.exception.ResourceNotFoundException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.dto.request.UserUpdateRequest;
import com.petlis.dto.response.UserInfoResponse;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.entity.business.User;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.UserRepository;
import com.petlis.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户服务实现类
 *
 * 实现用户信息查询、更新、头像上传等功能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final InfoHospitalRepository hospitalRepository;

    /**
     * 文件上传路径配置
     */
    @Value("${app.file.upload-path:/data/petlis/uploads}")
    private String uploadPath;

    /**
     * 文件访问URL前缀
     */
    @Value("${app.file.access-url:http://localhost:8080/api/files}")
    private String accessUrl;

    /**
     * 允许的图片类型
     */
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg",
            "image/jpg",
            "image/png"
    };

    /**
     * 最大文件大小 (2MB)
     */
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    @Override
    @Transactional(readOnly = true)
    public UserInfoResponse getUserProfile() {
        log.info("开始获取当前用户信息");

        // 从SecurityContext获取当前用户ID
        Integer userId = SecurityUtils.getCurrentUserId();
        log.debug("当前用户ID: {}", userId);

        // 查询用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));
        log.debug("查询用户信息成功, openid: {}, hospitalId: {}", user.getOpenid(), user.getHospitalId());

        // 查询医院信息
        UserInfoResponse.HospitalInfo hospitalInfo = null;
        Integer hospitalId = user.getHospitalId();
        if (hospitalId != null) {
            Optional<InfoHospital> hospital = hospitalRepository.findById(hospitalId);
            if (hospital.isPresent()) {
                InfoHospital h = hospital.get();
                hospitalInfo = UserInfoResponse.HospitalInfo.builder()
                        .hospitalId(h.getInfoHospitalId())
                        .hospitalName(h.getHospitalName())
                        .place(h.getPlace())
                        .tel(h.getTel())
                        .cityId(h.getCityId())
                        .build();
                log.debug("查询医院信息成功, hospitalName: {}", h.getHospitalName());
            }
        }

        // 转换为响应DTO
        UserInfoResponse response = UserInfoResponse.builder()
                .id(user.getId())
                .groupId(user.getGroupId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .avatarUrl(user.getAvatar())
                .level(user.getLevel())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .bio(user.getBio())
                .money(user.getMoney())
                .score(user.getScore())
                .successions(user.getSuccessions())
                .maxsuccessions(user.getMaxsuccessions())
                .prevtime(user.getPrevtime())
                .logintime(user.getLogintime())
                .createtime(user.getCreatetime())
                .updatetime(user.getUpdatetime())
                .openid(user.getOpenid())
                .hospitalId(user.getHospitalId())
                .hospital(hospitalInfo)
                .build();

        log.info("获取用户信息成功, userId: {}, nickname: {}", user.getId(), user.getNickname());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse updateUserProfile(UserUpdateRequest request) {
        log.info("开始更新用户信息");

        // 从SecurityContext获取当前用户ID
        Integer userId = SecurityUtils.getCurrentUserId();
        log.debug("当前用户ID: {}", userId);

        // 查询用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        // 更新可修改字段
        boolean updated = false;

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
            updated = true;
            log.debug("更新昵称: {}", request.getNickname());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatar(request.getAvatarUrl());
            updated = true;
            log.debug("更新头像URL: {}", request.getAvatarUrl());
        }

        if (request.getMobile() != null) {
            // 检查手机号是否被其他用户占用
            Optional<User> existingUser = userRepository.findByMobile(request.getMobile());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new BusinessException("该手机号已被其他用户使用");
            }
            user.setMobile(request.getMobile());
            updated = true;
            log.debug("更新手机号: {}", request.getMobile());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
            updated = true;
            log.debug("更新邮箱: {}", request.getEmail());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
            updated = true;
            log.debug("更新性别: {}", request.getGender());
        }

        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
            updated = true;
            log.debug("更新生日: {}", request.getBirthday());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
            updated = true;
            log.debug("更新格言: {}", request.getBio());
        }

        if (!updated) {
            log.info("没有需要更新的字段");
            return getUserProfile();
        }

        // 自动更新updatetime
        user.setUpdatetime((int) Instant.now().getEpochSecond());
        log.debug("更新updatetime: {}", user.getUpdatetime());

        // 保存更新
        userRepository.save(user);
        log.info("用户信息更新成功, userId: {}", userId);

        // 返回更新后的用户信息
        return getUserProfile();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(MultipartFile file) {
        log.info("开始上传头像");

        // 验证文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        boolean isValidType = false;
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equalsIgnoreCase(contentType)) {
                isValidType = true;
                break;
            }
        }
        if (!isValidType) {
            log.error("不支持的文件类型: {}", contentType);
            throw new BusinessException("只支持jpg、jpeg、png格式的图片");
        }

        // 验证文件大小
        long fileSize = file.getSize();
        if (fileSize > MAX_FILE_SIZE) {
            log.error("文件大小超过限制: {} bytes", fileSize);
            throw new BusinessException("图片大小不能超过2MB");
        }

        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            // 创建上传目录
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                log.debug("创建上传目录: {}, 结果: {}", uploadPath, created);
            }

            // 保存文件
            File destFile = new File(uploadDir, filename);
            file.transferTo(destFile);
            log.info("文件保存成功: {}", destFile.getAbsolutePath());

            // 构建访问URL
            String avatarUrl = accessUrl + "/avatars/" + filename;
            log.info("头像URL: {}", avatarUrl);

            // 更新用户头像
            Integer userId = SecurityUtils.getCurrentUserId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("用户", userId));
            user.setAvatar(avatarUrl);
            user.setUpdatetime((int) Instant.now().getEpochSecond());
            userRepository.save(user);

            log.info("头像上传成功, userId: {}, avatarUrl: {}", userId, avatarUrl);
            return avatarUrl;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }
}
