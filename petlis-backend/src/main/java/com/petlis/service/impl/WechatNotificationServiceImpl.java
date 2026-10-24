package com.petlis.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.petlis.common.enums.OrderStatusEnum;
import com.petlis.entity.NotificationLog;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.business.NotificationRecord;
import com.petlis.entity.business.User;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.ListDiagnosisProjectRepository;
import com.petlis.repository.NotificationLogRepository;
import com.petlis.repository.UserRepository;
import com.petlis.service.WechatNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * 微信通知服务实现类
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatNotificationServiceImpl implements WechatNotificationService {

    private final UserRepository userRepository;
    private final InfoHospitalRepository infoHospitalRepository;
    private final ListDiagnosisProjectRepository orderRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wechat.appid}")
    private String appId;

    @Value("${wechat.appsecret}")
    private String appSecret;

    @Value("${wechat.notification.template-id:}")
    private String templateId;

    @Value("${wechat.notification.enabled:false}")
    private Boolean notificationEnabled;

    /**
     * 发送订单状态变化通知
     *
     * @param notification 通知记录
     * @return 是否发送成功
     */
    @Override
    @Transactional
    public Boolean sendOrderStatusNotification(NotificationRecord notification) {
        log.info("准备发送订单状态通知, notificationId: {}, orderId: {}",
                notification.getId(), notification.getOrderId());

        // 创建日志记录
        NotificationLog logRecord = new NotificationLog();
        logRecord.setNotificationId(notification.getId());
        logRecord.setOrderId(notification.getOrderId());
        logRecord.setHospitalId(notification.getHospitalId());
        logRecord.setTemplateId(templateId);
        logRecord.setSendTime(LocalDateTime.now());

        try {
            // 检查是否启用通知功能
            if (!Boolean.TRUE.equals(notificationEnabled) || templateId == null || templateId.isEmpty()) {
                log.warn("微信通知功能未启用或模板ID未配置");
                logRecord.setSendStatus(2);
                logRecord.setErrorCode("NOT_CONFIGURED");
                logRecord.setErrorMessage("微信通知功能未启用或模板ID未配置");
                notificationLogRepository.save(logRecord);
                return false;
            }

            // 1. 获取医院信息
            InfoHospital hospital = infoHospitalRepository.findById(notification.getHospitalId())
                    .orElse(null);
            if (hospital == null) {
                log.error("医院不存在, hospitalId: {}", notification.getHospitalId());
                logRecord.setSendStatus(2);
                logRecord.setErrorCode("HOSPITAL_NOT_FOUND");
                logRecord.setErrorMessage("医院不存在, hospitalId: " + notification.getHospitalId());
                notificationLogRepository.save(logRecord);
                return false;
            }

            // 2. 获取医院管理员用户（需要openid）
            User adminUser = userRepository.findFirstByHospitalIdAndGroupId(
                    notification.getHospitalId(), 1)
                    .orElse(null);

            if (adminUser == null || adminUser.getOpenid() == null) {
                log.error("医院管理员或openid不存在, hospitalId: {}", notification.getHospitalId());
                logRecord.setSendStatus(2);
                logRecord.setErrorCode("USER_OR_OPENID_NOT_FOUND");
                logRecord.setErrorMessage("医院管理员或openid不存在");
                notificationLogRepository.save(logRecord);
                return false;
            }

            // 设置用户信息
            logRecord.setUserId(adminUser.getId());
            logRecord.setOpenid(adminUser.getOpenid());

            // 3. 获取订单信息
            ListDiagnosisProject order = orderRepository.findById(notification.getOrderId())
                    .orElse(null);
            if (order == null) {
                log.error("订单不存在, orderId: {}", notification.getOrderId());
                logRecord.setSendStatus(2);
                logRecord.setErrorCode("ORDER_NOT_FOUND");
                logRecord.setErrorMessage("订单不存在, orderId: " + notification.getOrderId());
                notificationLogRepository.save(logRecord);
                return false;
            }

            // 4. 获取access_token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取access_token失败");
                logRecord.setSendStatus(2);
                logRecord.setErrorCode("ACCESS_TOKEN_FAILED");
                logRecord.setErrorMessage("获取access_token失败");
                notificationLogRepository.save(logRecord);
                return false;
            }

            // 5. 构建消息内容
            JSONObject messageData = buildMessageData(notification, order, hospital);

            // 6. 发送订阅消息
            String messageId = sendTemplateMessage(
                    adminUser.getOpenid(),
                    templateId,
                    messageData,
                    "pages/order-detail/order-detail?id=" + notification.getOrderId()
            );

            if (messageId != null) {
                log.info("微信通知发送成功, messageId: {}", messageId);
                logRecord.setSendStatus(1);
                logRecord.setMessageId(messageId);
                notificationLogRepository.save(logRecord);
                return true;
            } else {
                log.error("微信通知发送失败");
                logRecord.setSendStatus(2);
                logRecord.setErrorCode("SEND_FAILED");
                logRecord.setErrorMessage("微信通知发送失败");
                notificationLogRepository.save(logRecord);
                return false;
            }

        } catch (Exception e) {
            log.error("发送订单状态通知异常", e);
            logRecord.setSendStatus(2);
            logRecord.setErrorCode("EXCEPTION");
            logRecord.setErrorMessage("发送异常: " + e.getMessage());
            notificationLogRepository.save(logRecord);
            return false;
        }
    }

    /**
     * 发送模板消息
     *
     * @param openid     用户openid
     * @param templateId 模板ID
     * @param data       模板数据
     * @param page       跳转页面
     * @return 消息ID
     */
    @Override
    public String sendTemplateMessage(String openid, String templateId, Object data, String page) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return null;
            }

            String url = "https://api.weixin.qq.com/wxa/templates/send?access_token=" + accessToken;

            JSONObject requestBody = new JSONObject();
            requestBody.set("touser", openid);
            requestBody.set("template_id", templateId);
            requestBody.set("page", page);
            requestBody.set("data", data);
            requestBody.set("miniprogramState", "developer"); // 开发版

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JSONObject responseBody = JSONUtil.parseObj(response.getBody());
            Integer errcode = responseBody.getInt("errcode", -1);

            if (errcode == 0) {
                return responseBody.getStr("msgid");
            } else {
                log.error("发送微信消息失败, errcode: {}, errmsg: {}",
                        errcode, responseBody.getStr("errmsg"));
                return null;
            }

        } catch (Exception e) {
            log.error("发送微信消息异常", e);
            return null;
        }
    }

    /**
     * 获取微信access_token
     *
     * @return access_token
     */
    private String getAccessToken() {
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential" +
                    "&appid=" + appId +
                    "&secret=" + appSecret;

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject responseBody = JSONUtil.parseObj(response.getBody());

            Integer errcode = responseBody.getInt("errcode", -1);
            if (errcode == 0) {
                return responseBody.getStr("access_token");
            } else {
                log.error("获取access_token失败, errcode: {}, errmsg: {}",
                        errcode, responseBody.getStr("errmsg"));
                return null;
            }

        } catch (Exception e) {
            log.error("获取access_token异常", e);
            return null;
        }
    }

    /**
     * 构建消息数据
     *
     * @param notification 通知记录
     * @param order        订单信息
     * @param hospital     医院信息
     * @return 消息数据
     */
    private JSONObject buildMessageData(NotificationRecord notification, ListDiagnosisProject order, InfoHospital hospital) {
        JSONObject data = new JSONObject();

        // 获取状态描述
        OrderStatusEnum newStatus = OrderStatusEnum.fromCode(notification.getNewStatus());
        String statusDesc = newStatus != null ? newStatus.getDescription() : "未知状态";

        // 订单编号
        JSONObject thing1 = new JSONObject();
        thing1.set("value", order.getUserNO() != null ? order.getUserNO() : "未知");
        data.set("thing1", thing1);

        // 宠物名称
        JSONObject thing2 = new JSONObject();
        thing2.set("value", order.getAnimalName() != null ? order.getAnimalName() : "未知");
        data.set("thing2", thing2);

        // 订单状态
        JSONObject thing3 = new JSONObject();
        thing3.set("value", statusDesc);
        data.set("thing3", thing3);

        // 状态变化时间
        JSONObject date4 = new JSONObject();
        date4.set("value", notification.getOperationTime().toString().replace("T", " ").substring(0, 19));
        data.set("date4", date4);

        return data;
    }
}
