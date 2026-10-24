package com.petlis.service.impl;

import com.petlis.common.enums.OrderStatusEnum;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.config.WeChatPayConfig;
import com.petlis.dto.request.PaymentCreateRequest;
import com.petlis.dto.response.PaymentResponse;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.business.User;
import com.petlis.entity.business.WeChatPayment;
import com.petlis.repository.ListDiagnosisProjectRepository;
import com.petlis.repository.UserRepository;
import com.petlis.repository.WeChatPaymentRepository;
import com.petlis.service.PaymentService;
import com.petlis.util.WeChatPayHttpClient;
import com.petlis.util.WeChatPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 微信支付服务实现类
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WeChatPaymentRepository weChatPaymentRepository;
    private final ListDiagnosisProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WeChatPayConfig weChatPayConfig;
    private final WeChatPayHttpClient weChatPayHttpClient;

    @Override
    @Transactional(rollbackOn = Exception.class)
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        log.info("创建支付订单, 订单ID: {}", request.getOrderId());

        // 1. 从JWT token中获取当前用户ID
        Integer userId = SecurityUtils.getCurrentUserId();
        log.info("从token获取用户ID: {}", userId);

        // 2. 查询用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 3. 查询业务订单
        ListDiagnosisProject order = projectRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException("订单不存在"));

        // 4. 计算支付金额
        BigDecimal amount = (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0)
                ? request.getAmount()
                : BigDecimal.valueOf(order.getTestFee());

        // 5. 生成支付订单号
        String paymentNo = WeChatPayUtil.generatePaymentNo();

        // 6. 创建支付记录
        WeChatPayment payment = new WeChatPayment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(request.getOrderId());
        payment.setUserId(user.getId()); // 从token获取的用户ID
        payment.setHospitalId(order.getInfoHospitalId()); // 从订单获取医院ID
        payment.setAmount(amount);
        payment.setTotalFee(WeChatPayUtil.yuanToFen(amount.doubleValue()));
        payment.setStatus(0); // 未支付
        payment.setPayType("JSAPI");
        payment.setTradeType("JSAPI");
        payment.setOpenid(user.getOpenid()); // 从用户信息获取openid
        payment.setBody("检测服务-" + order.getAnimalName());
        payment.setDetail(order.getAnimalName() + "的检测项目");
        payment.setAttach(request.getOrderId().toString());
        payment.setOutTradeNo(paymentNo);
        payment.setCreateAt(new Date());

        weChatPaymentRepository.save(payment);

        // 6. 判断是否为Mock模式
        log.info("微信支付配置 - Mock模式启用状态: {}", weChatPayConfig.getMockEnabled());
        if (Boolean.TRUE.equals(weChatPayConfig.getMockEnabled())) {
            log.info("检测到Mock模式启用，跳过真实微信支付API调用");
            return createMockPaymentResponse(paymentNo, request.getOrderId(), amount);
        }

        // 7. 调用微信统一下单API
        try {
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mch_id", weChatPayConfig.getMchId());
            params.put("nonce_str", WeChatPayUtil.generateNonceStr());
            params.put("body", payment.getBody());
            params.put("out_trade_no", paymentNo);
            params.put("total_fee", String.valueOf(payment.getTotalFee()));
            params.put("spbill_create_ip", "127.0.0.1"); // TODO: 实际应该获取真实IP
            params.put("notify_url", weChatPayConfig.getNotifyUrl());
            params.put("trade_type", "JSAPI");
            params.put("openid", request.getOpenid());

            // 生成签名
            String sign = WeChatPayUtil.generateMD5Sign(params, weChatPayConfig.getApiKey());
            params.put("sign", sign);

            // 转换为XML
            String xmlRequest = weChatPayHttpClient.mapToXml(params);
            log.info("微信统一下单请求: {}", xmlRequest);

            // 发送请求
            String xmlResponse = weChatPayHttpClient.postXml(
                "https://api.mch.weixin.qq.com/pay/unifiedorder",
                xmlRequest
            );
            log.info("微信统一下单响应: {}", xmlResponse);

            // 解析响应
            Map<String, String> responseMap = weChatPayHttpClient.xmlToMap(xmlResponse);

            String returnCode = responseMap.get("return_code");
            String returnMsg = responseMap.get("return_msg");

            if ("SUCCESS".equals(returnCode)) {
                String resultCode = responseMap.get("result_code");

                if ("SUCCESS".equals(resultCode)) {
                    // 成功，获取prepay_id
                    String prepayId = responseMap.get("prepay_id");

                    // 更新支付记录
                    payment.setPrepayId(prepayId);
                    weChatPaymentRepository.save(payment);

                    // 构建前端支付参数
                    String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
                    String nonceStr = WeChatPayUtil.generateNonceStr();
                    String packageStr = "prepay_id=" + prepayId;

                    // 前端支付签名
                    Map<String, String> paySignParams = new HashMap<>();
                    paySignParams.put("appId", weChatPayConfig.getAppId());
                    paySignParams.put("timeStamp", timeStamp);
                    paySignParams.put("nonceStr", nonceStr);
                    paySignParams.put("package", packageStr);
                    paySignParams.put("signType", "MD5");

                    String paySign = WeChatPayUtil.generateMD5Sign(paySignParams, weChatPayConfig.getApiKey());

                    // 构建响应
                    PaymentResponse response = new PaymentResponse();
                    response.setPaymentNo(paymentNo);
                    response.setOrderId(request.getOrderId());
                    response.setAmount(String.valueOf(amount));
                    response.setStatus(0);
                    response.setStatusDesc("未支付");
                    response.setPrepayId(prepayId);
                    response.setTimeStamp(timeStamp);
                    response.setNonceStr(nonceStr);
                    response.setSignType("MD5");
                    response.setPackageValue(packageStr);
                    response.setPaySign(paySign);

                    log.info("统一下单成功, 支付订单号: {}, prepay_id: {}", paymentNo, prepayId);
                    return response;
                } else {
                    // 业务失败
                    String errCode = responseMap.get("err_code");
                    String errCodeDes = responseMap.get("err_code_des");
                    log.error("微信统一下单业务失败: {} - {}", errCode, errCodeDes);
                    throw new BusinessException("支付下单失败: " + errCodeDes);
                }
            } else {
                // 通信失败
                log.error("微信统一下单通信失败: {}", returnMsg);
                throw new BusinessException("支付服务不可用: " + returnMsg);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信统一下单API异常", e);
            throw new BusinessException("支付下单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public boolean handlePaymentCallback(String paymentNo, String transactionId) {
        log.info("处理支付回调, 支付订单号: {}, 微信交易号: {}", paymentNo, transactionId);

        try {
            // 1. 查询支付记录
            WeChatPayment payment = weChatPaymentRepository.findByPaymentNo(paymentNo)
                    .orElseThrow(() -> new BusinessException("支付订单不存在"));

            // 2. 检查是否已处理
            if (payment.getStatus() == 1) {
                log.warn("支付订单已处理, 支付订单号: {}", paymentNo);
                return true;
            }

            // 3. 更新支付状态
            payment.setStatus(1); // 已支付
            payment.setTransactionId(transactionId);
            payment.setTradeState("SUCCESS");
            payment.setPaidAt(new Date());
            payment.setNotifyTime(new Date());

            weChatPaymentRepository.save(payment);

            // 4. 更新业务订单的支付状态
            ListDiagnosisProject order = projectRepository.findById(payment.getOrderId())
                    .orElse(null);
            if (order != null) {
                order.setIsPayMoney(1); // 已支付

                // 如果订单当前是待支付状态，支付成功后流转到待审核状态
                if (order.getSampleStatus().equals(OrderStatusEnum.PENDING_PAYMENT.getCode())) {
                    order.setSampleStatus(OrderStatusEnum.PENDING_REVIEW.getCode());
                    log.info("支付成功，订单状态从待支付更新为待审核, orderId: {}", payment.getOrderId());
                }

                projectRepository.save(order);
            }

            log.info("支付回调处理成功, 支付订单号: {}", paymentNo);
            return true;
        } catch (Exception e) {
            log.error("处理支付回调失败", e);
            return false;
        }
    }

    @Override
    public Integer getPaymentStatus(String paymentNo) {
        return weChatPaymentRepository.findByPaymentNo(paymentNo)
                .map(WeChatPayment::getStatus)
                .orElse(null);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void closePayment(String paymentNo) {
        log.info("关闭支付订单, 支付订单号: {}", paymentNo);

        WeChatPayment payment = weChatPaymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException("支付订单不存在"));

        if (payment.getStatus() != 0) {
            throw new BusinessException("只能关闭未支付的订单");
        }

        payment.setStatus(2); // 已关闭
        payment.setUpdateAt(new Date());
        weChatPaymentRepository.save(payment);

        // TODO: 实际项目中需要调用微信关闭订单API
        log.info("支付订单已关闭, 支付订单号: {}", paymentNo);
    }

    /**
     * 创建模拟支付响应（开发测试用）
     *
     * @param paymentNo 支付订单号
     * @param orderId 业务订单ID
     * @param amount 支付金额
     * @return 模拟的支付响应
     */
    private PaymentResponse createMockPaymentResponse(String paymentNo, Integer orderId, BigDecimal amount) {
        // 生成模拟的prepay_id
        String mockPrepayId = "mock_prepay_id_" + System.currentTimeMillis();

        // 更新支付记录
        WeChatPayment payment = weChatPaymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException("支付订单不存在"));
        payment.setPrepayId(mockPrepayId);
        weChatPaymentRepository.save(payment);

        // 构建前端支付参数（模拟）
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = WeChatPayUtil.generateNonceStr();
        String packageStr = "prepay_id=" + mockPrepayId;

        // 前端支付签名（使用mock签名）
        Map<String, String> paySignParams = new HashMap<>();
        paySignParams.put("appId", weChatPayConfig.getAppId());
        paySignParams.put("timeStamp", timeStamp);
        paySignParams.put("nonceStr", nonceStr);
        paySignParams.put("package", packageStr);
        paySignParams.put("signType", "MD5");

        String paySign = "MOCK_SIGNATURE_" + System.currentTimeMillis();

        // 构建响应
        PaymentResponse response = new PaymentResponse();
        response.setPaymentNo(paymentNo);
        response.setOrderId(orderId);
        response.setAmount(String.valueOf(amount));
        response.setStatus(0);
        response.setStatusDesc("未支付（Mock模式）");
        response.setPrepayId(mockPrepayId);
        response.setTimeStamp(timeStamp);
        response.setNonceStr(nonceStr);
        response.setSignType("MD5");
        response.setPackageValue(packageStr);
        response.setPaySign(paySign);

        log.info("Mock支付订单创建成功, 支付订单号: {}, prepay_id: {}", paymentNo, mockPrepayId);
        log.info("Mock支付响应对象: {}", response);
        log.warn("⚠️ 当前为Mock模式，不会真实调用微信支付API。支付流程仅为模拟！");
        return response;
    }
}
