package com.petlis.entity.business;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 微信支付记录实体类
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "lin_wechat_payment")
public class WeChatPayment {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /**
     * 支付订单号（商户侧）
     */
    @Column(name = "payment_no", nullable = false, length = 64, unique = true)
    private String paymentNo;

    /**
     * 业务订单ID（关联lin_lb_listdiagnosisproject）
     */
    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /**
     * 医院ID
     */
    @Column(name = "hospital_id", nullable = false)
    private Integer hospitalId;

    /**
     * 支付金额（元）
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 支付金额（分）
     */
    @Column(name = "total_fee", nullable = false)
    private Integer totalFee;

    /**
     * 支付状态 (0=未支付, 1=已支付, 2=已关闭, 3=已退款)
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * 微信支付订单号
     */
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    /**
     * 微信预支付ID
     */
    @Column(name = "prepay_id", length = 64)
    private String prepayId;

    /**
     * 支付类型 (JSAPI=小程序支付)
     */
    @Column(name = "pay_type", length = 20)
    private String payType;

    /**
     * 微信用户openid
     */
    @Column(name = "openid", length = 128)
    private String openid;

    /**
     * 交易类型
     */
    @Column(name = "trade_type", length = 20)
    private String tradeType;

    /**
     * 交易状态
     */
    @Column(name = "trade_state", length = 20)
    private String tradeState;

    /**
     * 支付完成时间
     */
    @Column(name = "paid_at")
    private Date paidAt;

    /**
     * 创建时间
     */
    @Column(name = "create_at", nullable = false, updatable = false)
    private Date createAt;

    /**
     * 更新时间
     */
    @Column(name = "update_at")
    private Date updateAt;

    /**
     * 支付通知时间
     */
    @Column(name = "notify_time")
    private Date notifyTime;

    /**
     * 商品描述
     */
    @Column(name = "body", length = 255)
    private String body;

    /**
     * 商品详情
     */
    @Column(name = "detail", length = 255)
    private String detail;

    /**
     * 附加数据
     */
    @Column(name = "attach", length = 255)
    private String attach;

    /**
     * 商户订单号
     */
    @Column(name = "out_trade_no", length = 64)
    private String outTradeNo;
}
