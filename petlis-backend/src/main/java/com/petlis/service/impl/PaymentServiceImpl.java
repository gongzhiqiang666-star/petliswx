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
 private final BusinessNotifications notifications;


    private final WeChatPaymentRepository weChatPaymentRepository;
    private final ListDiagnosisProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WeChatPayConfig weChatPayConfig;
    private final WeChatPayHttpClient weChatPayHttpClient;
    private final CouponService couponService;

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
        ListDiagnosisProject order = projectRepository.findLockedById(request.getOrderId())
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!Objects.equals(order.getInfoHospitalId(), SecurityUtils.getCurrentHospitalId())
                || !Objects.equals(order.getCreateSampleId(), userId)) {
            throw new BusinessException(403, "无权支付此申请单");
        }
        if (Integer.valueOf(1).equals(order.getIsPayMoney())) throw new BusinessException("订单已支付");
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getSampleStatus())) {
            throw new BusinessException("请等待审核通过后支付");
        }
        if (user.getOpenid() == null || user.getOpenid().isBlank()) throw new BusinessException("请先绑定微信");
        // 金额和 OpenID 均以服务端为准，忽略前端传入的金额与身份。
        BigDecimal amount = BigDecimal.valueOf(order.getTestFee() == null ? 0 : order.getTestFee())
                .setScale(2, java.math.RoundingMode.UNNECESSARY);
        if (amount.signum() <= 0) throw new BusinessException("订单金额无效");
        var pending=weChatPaymentRepository.findByOrderIdAndStatus(order.getListDiagnosisProjectId(),0);
        if(pending.isPresent()){
            WeChatPayment previous=pending.get();
            if(!Objects.equals(previous.getUserId(),userId) || previous.getAmount().compareTo(amount)!=0)throw new BusinessException("原支付金额已变化，请先关闭原支付记录");
            if(previous.getPrepayId()==null || previous.getPrepayId().isBlank() || previous.getCreateAt()==null || previous.getCreateAt().before(new Date(System.currentTimeMillis()-110*60*1000L)))throw new BusinessException("支付记录已过期，请关闭原支付后重试");
            return resumePayment(previous);
        }
        if(!Boolean.TRUE.equals(weChatPayConfig.getMockEnabled()) && (weChatPayConfig.getApiKey()==null || weChatPayConfig.getApiKey().isBlank() || "1234567890".equals(weChatPayConfig.getMchId())))throw new BusinessException("微信支付尚未配置，请联系管理员");

        // 5. 生成支付订单号
        String paymentNo = WeChatPayUtil.generatePaymentNo();

        // 6. 创建支付记录
        WeChatPayment payment = new WeChatPayment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(request.getOrderId());
        payment.setUserId(user.getId()); // 从token获取的用户ID
        payment.setHospitalId(order.getInfoHospitalId()); // 从订单获取医院ID
        payment.setAmount(amount);
        payment.setTotalFee(amount.movePointRight(2).intValueExact());
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
            params.put("openid", user.getOpenid());

            // 生成签名
            String sign = WeChatPayUtil.generateMD5Sign(params, weChatPayConfig.getApiKey());
            params.put("sign", sign);

            // 转换为XML
            String xmlRequest = weChatPayHttpClient.mapToXml(params);
            log.debug("调用微信统一下单，支付单号: {}", paymentNo);

            // 发送请求
            String xmlResponse = weChatPayHttpClient.postXml(
                "https://api.mch.weixin.qq.com/pay/unifiedorder",
                xmlRequest
            );
            log.debug("微信统一下单响应已收到");

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

        WeChatPayment payment = weChatPaymentRepository.findLockedByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException("支付订单不存在"));
        if (Integer.valueOf(1).equals(payment.getStatus())) {
            if (!Objects.equals(payment.getTransactionId(), transactionId)) throw new BusinessException("交易号不一致");
            return true;
        }
        if (!Integer.valueOf(0).equals(payment.getStatus())) throw new BusinessException("支付订单已关闭");
        ListDiagnosisProject order = projectRepository.findLockedById(payment.getOrderId())
                .orElseThrow(() -> new BusinessException("业务订单不存在"));
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getSampleStatus())) {
            throw new BusinessException("订单不处于审核通过待支付状态");
        }
        payment.setStatus(1);
        payment.setTransactionId(transactionId);
        payment.setTradeState("SUCCESS");
        payment.setPaidAt(new Date());
        payment.setNotifyTime(new Date());
        order.setIsPayMoney(1);
        order.setSampleStatus(OrderStatusEnum.WAITING_REPORT.getCode());
        notifications.order(order.getListDiagnosisProjectId(),order.getCreateSampleId(),OrderStatusEnum.WAITING_REPORT.getCode());
        weChatPaymentRepository.save(payment);
        projectRepository.save(order);
        couponService.consume(payment.getOrderId());
        return true;
    }

    @Override
    public Integer getPaymentStatus(String paymentNo) {
        return ownedPayment(paymentNo).getStatus();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void closePayment(String paymentNo) {
        log.info("关闭支付订单, 支付订单号: {}", paymentNo);

        WeChatPayment payment = ownedPayment(paymentNo);

        if (payment.getStatus() != 0) {
            throw new BusinessException("只能关闭未支付的订单");
        }

        if (!Boolean.TRUE.equals(weChatPayConfig.getMockEnabled())) {
            Map<String, String> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mch_id", weChatPayConfig.getMchId());
            params.put("out_trade_no", paymentNo);
            params.put("nonce_str", WeChatPayUtil.generateNonceStr());
            params.put("sign", WeChatPayUtil.generateMD5Sign(params, weChatPayConfig.getApiKey()));
            Map<String, String> result = weChatPayHttpClient.xmlToMap(weChatPayHttpClient.postXml(
                    "https://api.mch.weixin.qq.com/pay/closeorder", weChatPayHttpClient.mapToXml(params)));
            if (!"SUCCESS".equals(result.get("return_code")) || !"SUCCESS".equals(result.get("result_code"))) {
                throw new BusinessException("微信关闭订单失败，请查询支付状态后重试");
            }
        }
        payment.setStatus(2); // 已关闭
        payment.setUpdateAt(new Date());
        weChatPaymentRepository.save(payment);

        log.info("支付订单已关闭, 支付订单号: {}", paymentNo);
    }

    private WeChatPayment ownedPayment(String paymentNo) {
        WeChatPayment payment = weChatPaymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException("支付订单不存在"));
        if (!Objects.equals(payment.getUserId(), SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(403, "无权访问此支付记录");
        }
        return payment;
    }

    /**
     * 创建模拟支付响应（开发测试用）
     *
     * @param paymentNo 支付订单号
     * @param orderId 业务订单ID
     * @param amount 支付金额
     * @return 模拟的支付响应
     */
    private PaymentResponse resumePayment(WeChatPayment payment) {
        boolean mock=payment.getPrepayId().startsWith("mock_");
        if(mock && !Boolean.TRUE.equals(weChatPayConfig.getMockEnabled()))throw new BusinessException("请关闭原开发支付记录后重试");
        String timestamp=String.valueOf(System.currentTimeMillis()/1000),nonce=WeChatPayUtil.generateNonceStr(),packageValue="prepay_id="+payment.getPrepayId();
        var params=new HashMap<String,String>();params.put("appId",weChatPayConfig.getAppId());params.put("timeStamp",timestamp);params.put("nonceStr",nonce);params.put("package",packageValue);params.put("signType","MD5");
        PaymentResponse response=new PaymentResponse();response.setPaymentNo(payment.getPaymentNo());response.setOrderId(payment.getOrderId());response.setAmount(payment.getAmount().toPlainString());response.setPrepayId(payment.getPrepayId());response.setTimeStamp(timestamp);response.setNonceStr(nonce);response.setPackageValue(packageValue);response.setSignType("MD5");response.setPaySign(mock?"MOCK_SIGNATURE":WeChatPayUtil.generateMD5Sign(params,weChatPayConfig.getApiKey()));response.setStatus(0);response.setMock(mock);response.setStatusDesc(mock?"未支付（开发模式）":"未支付");return response;
    }

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
        response.setMock(true);
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
