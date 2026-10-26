package com.petlis.service.impl;

import com.petlis.common.enums.OrderStatusEnum;
import java.util.List;
import java.util.stream.Collectors;
import com.petlis.common.exception.BusinessException;
import com.petlis.dto.response.NotificationResponse;
import com.petlis.entity.business.NotificationRecord;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.entity.business.User;
import com.petlis.mapper.NotificationMapper;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.NotificationRepository;
import com.petlis.repository.UserRepository;
import com.petlis.service.NotificationService;
import com.petlis.service.WechatNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知Service实现类
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.petlis.common.security.ReviewPermission permission;
    private final InfoHospitalRepository infoHospitalRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final WechatNotificationService wechatNotificationService;

    /**
     * 查询订单的通知记录列表
     *
     * @param orderId 订单ID
     * @return 通知记录列表
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getOrderNotifications(Integer orderId) {
        log.info("查询订单通知记录, orderId: {}", orderId);

        requireOrder(orderId);
        // 构建分页参数
        Sort sort = Sort.by(Sort.Direction.DESC, "operationTime");
        Pageable pageable = PageRequest.of(0, 100, sort); // 默认返回前100条

        // 查询通知记录
        List<NotificationRecord> records = notificationRepository.findByOrderId(orderId);

        // 转换为DTO
        List<NotificationResponse> responses = records.stream()
            .map(record -> {
                requireHospital(record.getHospitalId());
        NotificationResponse response = notificationMapper.toResponse(record);
                loadRelatedInfo(response, record);
                return response;
            })
            .collect(Collectors.toList());

        // 构建分页结果
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        List<NotificationResponse> pageContent = responses.subList(start, end);

        return new PageImpl<>(pageContent, pageable, responses.size());
    }

    /**
     * 查询医院的通知记录列表(分页)
     *
     * @param hospitalId 医院ID
     * @param pageNum    页码
     * @param pageSize   每页大小
     * @return 通知记录分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getHospitalNotifications(Integer hospitalId, Integer pageNum, Integer pageSize) {
        requireHospital(hospitalId);
        log.info("查询医院通知记录, hospitalId: {}, pageNum: {}, pageSize: {}", hospitalId, pageNum, pageSize);

        // 构建分页参数
        Sort sort = Sort.by(Sort.Direction.DESC, "operationTime");
        Pageable pageable = PageRequest.of(
                pageNum != null ? pageNum : 0,
                pageSize != null ? pageSize : 10,
                sort
        );

        // 查询通知记录
        Page<NotificationRecord> recordPage = notificationRepository.findByHospitalId(hospitalId, pageable);

        // 转换为DTO
        return recordPage.map(record -> {
            NotificationResponse response = notificationMapper.toResponse(record);
            loadRelatedInfo(response, record);
            return response;
        });
    }

    /**
     * 获取通知详情
     *
     * @param notificationId 通知ID
     * @return 通知详情
     */
    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationDetail(Integer notificationId) {
        log.info("获取通知详情, notificationId: {}", notificationId);

        NotificationRecord record = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知记录不存在"));

        requireHospital(record.getHospitalId());
        NotificationResponse response = notificationMapper.toResponse(record);
        loadRelatedInfo(response, record);

        return response;
    }

    /**
     * 处理待发送的微信通知
     * 此方法由定时任务调用
     *
     * @return 处理的通知数量
     */
    @Override
    @Transactional
    public Integer processPendingNotifications() {
        permission.requireReviewer();
        log.info("开始处理待发送的微信通知");

        // 查询待发送的通知
        var pendingNotifications = notificationRepository.findPendingNotifications();
        log.info("找到 {} 条待发送通知", pendingNotifications.size());

        int processedCount = 0;
        int successCount = 0;
        int failCount = 0;

        for (NotificationRecord record : pendingNotifications) {
            try {
                // 调用微信通知服务发送通知
                Boolean success = wechatNotificationService.sendOrderStatusNotification(record);

                if (Boolean.TRUE.equals(success)) {
                    record.setWechatSentStatus(1); // 已发送
                    record.setErrorMessage(null);
                    successCount++;
                } else {
                    record.setWechatSentStatus(2); // 发送失败
                    record.setErrorMessage("微信通知发送失败");
                    failCount++;
                }

                notificationRepository.save(record);
                processedCount++;

            } catch (Exception e) {
                log.error("发送微信通知失败, notificationId: {}", record.getId(), e);
                record.setWechatSentStatus(2);
                record.setErrorMessage(e.getMessage());
                notificationRepository.save(record);
                processedCount++;
                failCount++;
            }
        }

        log.info("处理完成, 共处理 {} 条通知, 成功 {} 条, 失败 {} 条", processedCount, successCount, failCount);
        return processedCount;
    }

    /**
     * 重试发送失败的通知
     *
     * @param notificationId 通知ID
     * @return 是否重试成功
     */
    @Override
    @Transactional
    public Boolean retryNotification(Integer notificationId) {
        permission.requireReviewer();
        log.info("重试发送通知, notificationId: {}", notificationId);

        NotificationRecord record = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知记录不存在"));

        if (record.getWechatSentStatus() != 2) {
            throw new BusinessException("只能重试发送失败的通知");
        }

        try {
            // 重置为待发送状态
            record.setWechatSentStatus(0);
            record.setErrorMessage(null);
            notificationRepository.save(record);

            // 重新发送
            Boolean success = wechatNotificationService.sendOrderStatusNotification(record);

            if (Boolean.TRUE.equals(success)) {
                record.setWechatSentStatus(1);
                record.setErrorMessage(null);
            } else {
                record.setWechatSentStatus(2);
                record.setErrorMessage("重试发送失败");
            }

            notificationRepository.save(record);
            return success;

        } catch (Exception e) {
            log.error("重试发送微信通知失败, notificationId: {}", notificationId, e);
            record.setErrorMessage(e.getMessage());
            notificationRepository.save(record);
            return false;
        }
    }

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     */
    @Override
    @Transactional
    public void markAsRead(Integer notificationId) {
        log.info("标记通知为已读, notificationId: {}", notificationId);

        NotificationRecord record = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知记录不存在"));

        requireHospital(record.getHospitalId());
        jdbc.update("INSERT IGNORE INTO lin_notification_receipt(user_id,notification_id) VALUES(?,?)",com.petlis.common.security.SecurityUtils.getCurrentUserId(),notificationId);

    }

    /**
     * 统计医院未读通知数量
     *
     * @param hospitalId 医院ID
     * @return 未读通知数量
     */
    @Override
    @Transactional(readOnly = true)
    public Long countUnreadNotifications(Integer hospitalId) {
        requireHospital(hospitalId);
        return jdbc.queryForObject("SELECT COUNT(*) FROM lin_lb_notification n LEFT JOIN lin_notification_receipt r ON r.notification_id=n.id AND r.user_id=? WHERE n.hospital_id=? AND r.user_id IS NULL",Long.class,com.petlis.common.security.SecurityUtils.getCurrentUserId(),hospitalId);
    }

    /**
     * 加载关联信息到Response
     *
     * @param response 响应对象
     * @param record   通知记录
     */
    private void requireHospital(Integer hospitalId) {
        if(hospitalId==null || !java.util.Objects.equals(hospitalId,com.petlis.common.security.SecurityUtils.getCurrentHospitalId()))throw new BusinessException(403,"无权访问此通知");
    }
    private void requireOrder(Integer orderId) {
        Boolean allowed=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? AND InfoHospitalId=?)",Boolean.class,orderId,com.petlis.common.security.SecurityUtils.getCurrentHospitalId());
        if(!Boolean.TRUE.equals(allowed))throw new BusinessException(403,"无权访问此订单通知");
    }

    private void loadRelatedInfo(NotificationResponse response, NotificationRecord record) {
        // 加载医院名称
        if (record.getHospitalId() != null) {
            InfoHospital hospital = infoHospitalRepository.findById(record.getHospitalId()).orElse(null);
            if (hospital != null) {
                response.setHospitalName(hospital.getHospitalName());
            }
        }

        // 加载操作人名称
        if (record.getOperatorId() != null) {
            User user = userRepository.findById(record.getOperatorId()).orElse(null);
            if (user != null) {
                response.setOperatorName(user.getNickname());
            }
        }

        // 加载状态描述
        if (record.getOldStatus() != null) {
            OrderStatusEnum oldStatus = OrderStatusEnum.fromCode(record.getOldStatus());
            response.setOldStatusLabel(oldStatus != null ? oldStatus.getDescription() : "");
        }

        if (record.getNewStatus() != null) {
            OrderStatusEnum newStatus = OrderStatusEnum.fromCode(record.getNewStatus());
            response.setNewStatusLabel(newStatus != null ? newStatus.getDescription() : "");
            response.setNotificationMessage("送检单 #"+record.getOrderId()+" 状态："+(newStatus!=null?newStatus.getDescription():"已更新"));
        }

        // 加载微信发送状态描述
        response.setWechatSentStatusLabel(getWechatSentStatusLabel(record.getWechatSentStatus()));
    }

    /**
     * 获取微信发送状态描述
     *
     * @param status 状态码
     * @return 状态描述
     */
    private String getWechatSentStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0:
                return "待发送";
            case 1:
                return "已发送";
            case 2:
                return "发送失败";
            default:
                return "";
        }
    }
}
