package com.petlis.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 微信支付工具类
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Component
public class WeChatPayUtil {

    /**
     * 生成随机字符串
     */
    public static String generateNonceStr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * 生成MD5签名
     */
    public static String generateMD5Sign(Map<String, String> params, String apiKey) {
        try {
            // 1. 参数排序
            TreeMap<String, String> sortedParams = new TreeMap<>(params);

            // 2. 拼接字符串
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null && !"".equals(entry.getValue()) && !"sign".equals(entry.getKey())) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            sb.append("key=").append(apiKey);

            // 3. MD5加密
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            // 4. 转为大写十六进制
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            log.error("生成MD5签名失败", e);
            throw new RuntimeException("生成签名失败");
        }
    }

    /**
     * 生成HMAC-SHA256签名
     */
    public static String generateHMACSHA256Sign(Map<String, String> params, String apiKey) {
        try {
            // 1. 参数排序
            TreeMap<String, String> sortedParams = new TreeMap<>(params);

            // 2. 拼接字符串
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null && !"".equals(entry.getValue()) && !"sign".equals(entry.getKey())) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            sb.append("key=").append(apiKey);

            // 3. HMAC-SHA256加密
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));

            // 4. 转为大写十六进制
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            log.error("生成HMAC-SHA256签名失败", e);
            throw new RuntimeException("生成签名失败");
        }
    }

    /**
     * 生成订单号（格式：PAY+时间戳+随机数）
     */
    public static String generatePaymentNo() {
        return "PAY" + System.currentTimeMillis() + (int)(Math.random() * 10000);
    }

    /**
     * 元转分
     */
    public static Integer yuanToFen(Double yuan) {
        if (yuan == null) {
            return 0;
        }
        return (int) (yuan * 100);
    }

    /**
     * 分转元
     */
    public static Double fenToYuan(Integer fen) {
        if (fen == null) {
            return 0.0;
        }
        return fen / 100.0;
    }
}
