package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.config.WeChatPayConfig;
import com.petlis.dto.response.PaymentResponse;
import com.petlis.util.WeChatPayHttpClient;
import com.petlis.util.WeChatPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
@RequiredArgsConstructor
public class WeChatPaymentGateway {
    private final WeChatPayConfig config;
    private final WeChatPayHttpClient http;

    public boolean mockEnabled() { return Boolean.TRUE.equals(config.getMockEnabled()); }

    public String create(String paymentNo, BigDecimal amount, String openid, String description) {
        if (mockEnabled()) return "mock_" + UUID.randomUUID().toString().replace("-", "");
        requireConfiguration();
        if (openid == null || openid.isBlank()) throw new BusinessException("请先在账号安全中绑定微信");
        Map<String,String> params = base(paymentNo);
        params.put("body", description);
        params.put("total_fee", String.valueOf(amount.movePointRight(2).intValueExact()));
        params.put("openid", openid);
        params.put("trade_type", "JSAPI");
        params.put("notify_url", config.getNotifyUrl());
        params.put("spbill_create_ip", "127.0.0.1");
        Map<String,String> response = request("unifiedorder", params);
        verifyResponse(response);
        String prepay = response.get("prepay_id");
        if (prepay == null || prepay.isBlank()) throw new BusinessException("微信未返回有效支付信息，请稍后重试");
        return prepay;
    }

    public PaymentResponse parameters(String paymentNo, BigDecimal amount, String prepayId, boolean paid) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentNo(paymentNo);
        response.setAmount(amount.toPlainString());
        response.setStatus(paid ? 1 : 0);
        response.setStatusDesc(paid ? "已支付" : "待支付");
        boolean mock = prepayId != null && prepayId.startsWith("mock_");
        response.setMock(mock);
        if (paid) return response;
        if (prepayId == null || prepayId.isBlank()) throw new BusinessException("支付信息未就绪，请重试");
        if (mock && !mockEnabled()) throw new BusinessException("请先关闭原开发支付记录");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = WeChatPayUtil.generateNonceStr();
        String packageValue = "prepay_id=" + prepayId;
        Map<String,String> values = new HashMap<>();
        values.put("appId", config.getAppId());
        values.put("timeStamp", timestamp);
        values.put("nonceStr", nonce);
        values.put("package", packageValue);
        values.put("signType", "MD5");
        response.setPrepayId(prepayId);
        response.setTimeStamp(timestamp);
        response.setNonceStr(nonce);
        response.setPackageValue(packageValue);
        response.setSignType("MD5");
        response.setPaySign(mock ? "MOCK_SIGNATURE" : WeChatPayUtil.generateMD5Sign(values, config.getApiKey()));
        return response;
    }

    public void close(String paymentNo, String prepayId) {
        if (prepayId != null && prepayId.startsWith("mock_")) {
            if (!mockEnabled()) throw new BusinessException("原开发支付记录不能用于正式支付");
            return;
        }
        requireConfiguration();
        Map<String,String> result = request("closeorder", base(paymentNo));
        verifyResponse(result);
    }

    private Map<String,String> base(String paymentNo) {
        Map<String,String> params = new HashMap<>();
        params.put("appid", config.getAppId());
        params.put("mch_id", config.getMchId());
        params.put("nonce_str", WeChatPayUtil.generateNonceStr());
        params.put("out_trade_no", paymentNo);
        return params;
    }

    private Map<String,String> request(String action, Map<String,String> params) {
        params.put("sign", WeChatPayUtil.generateMD5Sign(params, config.getApiKey()));
        try {
            return http.xmlToMap(http.postXml("https://api.mch.weixin.qq.com/pay/" + action, http.mapToXml(params)));
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException("微信支付服务暂不可用，请稍后重试"); }
    }

    private void verifyResponse(Map<String,String> result) {
        if (!"SUCCESS".equals(result.get("return_code")) || !"SUCCESS".equals(result.get("result_code")))
            throw new BusinessException("微信未接受本次支付操作，请查询支付状态后重试");
        if (!Objects.equals(config.getAppId(),result.get("appid")) || !Objects.equals(config.getMchId(),result.get("mch_id")))
            throw new BusinessException("微信返回的商户信息不一致");
        String expected = WeChatPayUtil.generateMD5Sign(result,config.getApiKey());
        String received = result.getOrDefault("sign", "").toUpperCase(Locale.ROOT);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),received.getBytes(StandardCharsets.US_ASCII)))
            throw new BusinessException("微信支付响应签名无效");
    }

    private void requireConfiguration() {
        if (config.getApiKey()==null || config.getApiKey().isBlank() || config.getAppId()==null
                || config.getMchId()==null || "1234567890".equals(config.getMchId())
                || config.getNotifyUrl()==null || !config.getNotifyUrl().startsWith("https://"))
            throw new BusinessException("微信支付商户与回调尚未配置，请联系管理员");
    }
}
