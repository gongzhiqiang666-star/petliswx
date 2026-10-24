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

    /**
     * 默认医院ID (新用户关联的默认医院)
     */
    private static final Integer DEFAULT_HOSPITAL_ID = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatLoginResponse wechatLogin(WechatLoginRequest request) {
        log.info("开始微信登录, code: {}", request.getCode());

        // 1. 调用微信接口获取openid
        WechatUtil.WechatLoginResponse wechatResponse;
        try {
            wechatResponse = wechatUtil.code2Session(request.getCode());
            log.info("微信登录成功, openid: {}", wechatResponse.openid());
        } catch (Exception e) {
            log.error("微信登录失败: {}", e.getMessage(), e);
            throw new BusinessException("微信登录失败: " + e.getMessage());
        }

        String openid = wechatResponse.openid();

        // 2. 查询用户是否存在
        Optional<User> existingUser = userRepository.findByOpenid(openid);
        User user;

        if (existingUser.isPresent()) {
            // 用户已存在,更新登录信息
            user = existingUser.get();
            log.info("用户已存在, userId: {}, hospitalId: {}", user.getId(), user.getHospitalId());

            // 更新登录时间
            user.setLogintime((int) Instant.now().getEpochSecond());

            // 保存更新
            userRepository.save(user);
        } else {
            // 用户不存在,创建新用户
            user = createNewUser(openid);
            log.info("创建新用户成功, userId: {}, hospitalId: {}", user.getId(), user.getHospitalId());
        }

        // 3. 查询医院信息
        Integer hospitalId = user.getHospitalId();
        if (hospitalId == null) {
            hospitalId = DEFAULT_HOSPITAL_ID;
            user.setHospitalId(hospitalId);
            userRepository.save(user);
        }

        // 查询医院详细信息（包括cityId）
        UserInfoResponse.HospitalInfo hospitalInfo = null;
        String hospitalName = null;
        Optional<InfoHospital> hospital = hospitalRepository.findById(hospitalId);
        if (hospital.isPresent()) {
            InfoHospital h = hospital.get();
            hospitalName = h.getHospitalName();

            // 验证cityId不为空（重要：确保token中有cityId）
            if (h.getCityId() == null || h.getCityId() == 0) {
                log.error("医院城市ID未设置, hospitalId: {}, hospitalName: {}", hospitalId, h.getHospitalName());
                throw new BusinessException("账号信息不完整：所属医院的城市未设置，请联系管理员维护医院信息");
            }

            hospitalInfo = UserInfoResponse.HospitalInfo.builder()
                    .hospitalId(h.getInfoHospitalId())
                    .hospitalName(h.getHospitalName())
                    .place(h.getPlace())
                    .tel(h.getTel())
                    .cityId(h.getCityId())
                    .build();
            log.debug("查询医院信息成功, hospitalName: {}, cityId: {}", h.getHospitalName(), h.getCityId());
        } else {
            log.warn("医院信息未找到, hospitalId: {}", hospitalId);
            throw new BusinessException("账号关联的医院不存在，hospitalId: " + hospitalId);
        }

        // 4. 生成JWT token（cityId已确保不为null）
        Integer cityId = hospitalInfo.getCityId();
        String token = jwtUtil.generateToken(user.getId(), openid, hospitalId, cityId);
        log.info("生成JWT token成功, userId: {}, hospitalId: {}, cityId: {}", user.getId(), hospitalId, cityId);

        // 5. 构建响应（包含完整的医院信息）
        return WechatLoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .hospitalId(hospitalId)
                .hospitalName(hospitalName)
                .hospital(hospitalInfo)
                .openid(openid)
                .build();
    }

    @Override
    public String refreshToken(String token) {
        log.info("刷新token");

        // 验证当前token
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("Token无效或已过期");
        }

        // 从token中提取用户信息
        Integer userId = jwtUtil.getUserIdFromToken(token);
        String openid = jwtUtil.getOpenidFromToken(token);
        Integer hospitalId = jwtUtil.getHospitalIdFromToken(token);
        Integer cityId = jwtUtil.getCityIdFromToken(token);

        // 查询用户是否存在
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new BusinessException("用户不存在");
        }

        // 生成新token
        String newToken = jwtUtil.generateToken(userId, openid, hospitalId, cityId);
        log.info("刷新token成功, userId: {}", userId);

        return newToken;
    }

    @Override
    public void logout(String token) {
        log.info("用户登出");

        // 验证token
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("Token无效或已过期");
        }

        Integer userId = jwtUtil.getUserIdFromToken(token);
        log.info("用户登出成功, userId: {}", userId);

        // TODO: 可以将token加入黑名单(Redis)
        // 目前仅做日志记录
    }

    /**
     * 创建新用户
     *
     * @param openid 微信openid
     * @return 新创建的用户
     */
    private User createNewUser(String openid) {
        User user = new User();
        int currentTime = (int) Instant.now().getEpochSecond();

        // 设置必填字段
        user.setGroupId(0);
        user.setUsername(openid); // 使用openid作为用户名
        user.setNickname("微信用户");
        user.setLevel(0);
        user.setGender(0);
        user.setMoney("0.00");
        user.setScore(0);
        user.setSuccessions(1);
        user.setMaxsuccessions(1);
        user.setLogintime(currentTime);
        user.setLoginfailure(0);
        user.setJointime(currentTime);
        user.setCreatetime(currentTime);
        user.setUpdatetime(currentTime);
        user.setStatus("normal"); // 正常状态
        user.setOpenid(openid);
        user.setHospitalId(DEFAULT_HOSPITAL_ID);

        // 保存用户
        return userRepository.save(user);
    }
}
