package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.config.WeChatPayConfig;
import com.petlis.entity.business.WeChatPayment;
import com.petlis.repository.WeChatPaymentRepository;
import com.petlis.service.PaymentService;
import com.petlis.util.WeChatPayHttpClient;
import com.petlis.util.WeChatPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentNotificationService {
    private final WeChatPayConfig config;
    private final WeChatPayHttpClient client;
    private final WeChatPaymentRepository repository;
    private final PaymentService paymentService;
    private final RechargeService rechargeService;

    public void handle(String xml) {
        if (xml == null || xml.length() > 65536) throw new BusinessException("回调内容无效");
        Map<String, String> values = client.xmlToMap(xml);
        verify(values);
        String paymentNo = required(values, "out_trade_no");
        int totalFee;
        try { totalFee = Integer.parseInt(required(values, "total_fee")); }
        catch (NumberFormatException e) { throw new BusinessException("回调金额无效"); }
        if(paymentNo.startsWith("RC")) {
            rechargeService.verifiedCallback(paymentNo,totalFee,required(values,"transaction_id"));
            return;
        }
        WeChatPayment payment = repository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException("支付单不存在"));
        if (!Objects.equals(payment.getTotalFee(), totalFee)) throw new BusinessException("回调金额不一致");
        paymentService.handlePaymentCallback(paymentNo, required(values, "transaction_id"));
    }

    public void verify(Map<String, String> values) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) throw new BusinessException("支付密钥未配置");
        if (!"SUCCESS".equals(values.get("return_code")) || !"SUCCESS".equals(values.get("result_code"))) {
            throw new BusinessException("支付未成功");
        }
        if (config.getAppId() == null || config.getMchId() == null
                || !Objects.equals(config.getAppId(), values.get("appid"))
                || !Objects.equals(config.getMchId(), values.get("mch_id"))) throw new BusinessException("商户信息不一致");
        String algorithm = values.getOrDefault("sign_type", "MD5");
        String expected = switch (algorithm) {
            case "MD5" -> WeChatPayUtil.generateMD5Sign(values, config.getApiKey());
            case "HMAC-SHA256" -> WeChatPayUtil.generateHMACSHA256Sign(values, config.getApiKey());
            default -> throw new BusinessException("签名算法不支持");
        };
        String received = required(values, "sign");
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), received.toUpperCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException("支付签名无效");
        }
    }
    private String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) throw new BusinessException("回调字段缺失");
        return value;
    }
}
