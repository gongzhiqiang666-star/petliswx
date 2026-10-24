package com.petlis.service;

import com.petlis.entity.business.NotificationRecord;

/**
 * 微信通知服务接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
public interface WechatNotificationService {

    /**
     * 发送订单状态变化通知
     *
     * @param notification 通知记录
     * @return 是否发送成功
     */
    Boolean sendOrderStatusNotification(NotificationRecord notification);

    /**
     * 发送模板消息（备用）
     *
     * @param openid    用户openid
     * @param templateId 模板ID
     * @param data      模板数据
     * @param page      跳转页面
     * @return 消息ID
     */
    String sendTemplateMessage(String openid, String templateId, Object data, String page);
}
