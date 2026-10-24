package com.petlis.service;

import com.petlis.dto.request.PaymentCreateRequest;
import com.petlis.dto.response.PaymentResponse;

/**
 * 支付服务接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
public interface PaymentService {

    /**
     * 创建支付订单
     *
     * @param request 支付请求
     * @return 支付响应
     */
    PaymentResponse createPayment(PaymentCreateRequest request);

    /**
     * 处理微信支付回调
     *
     * @param paymentNo 支付订单号
     * @param transactionId 微信交易号
     * @return 处理结果
     */
    boolean handlePaymentCallback(String paymentNo, String transactionId);

    /**
     * 查询支付状态
     *
     * @param paymentNo 支付订单号
     * @return 支付状态
     */
    Integer getPaymentStatus(String paymentNo);

    /**
     * 关闭支付订单
     *
     * @param paymentNo 支付订单号
     */
    void closePayment(String paymentNo);
}
