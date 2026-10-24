package com.petlis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.pay")
public class WeChatPayConfig {

    /**
     * 微信小程序appId
     */
    private String appId;

    /**
     * 微信支付商户号
     */
    private String mchId;

    /**
     * 微信支付API密钥
     */
    private String apiKey;

    /**
     * 微信支付APIv3密钥
     */
    private String apiV3Key;

    /**
     * 微信支付证书序列号
     */
    private String serialNo;

    /**
     * 微信支付证书路径
     */
    private String certPath;

    /**
     * 支付通知回调URL
     */
    private String notifyUrl;

    /**
     * 支付类型（JSAPI=小程序支付）
     */
    private String tradeType = "JSAPI";

    /**
     * Mock模式开关（开发测试用）
     * true: 模拟支付，不调用真实微信API
     * false: 调用真实微信支付API
     */
    private Boolean mockEnabled = false;
}
