package com.petlis.repository;

import com.petlis.entity.business.WeChatPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 微信支付Repository
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface WeChatPaymentRepository extends JpaRepository<WeChatPayment, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from WeChatPayment p where p.paymentNo = :paymentNo")
    Optional<WeChatPayment> findLockedByPaymentNo(@org.springframework.data.repository.query.Param("paymentNo") String paymentNo);


    /**
     * 根据支付订单号查询
     */
    Optional<WeChatPayment> findByPaymentNo(String paymentNo);

    /**
     * 根据业务订单ID查询
     */
    Optional<WeChatPayment> findByOrderId(Integer orderId);

    /**
     * 根据业务订单ID和支付状态查询
     */
    Optional<WeChatPayment> findByOrderIdAndStatus(Integer orderId, Integer status);

    /**
     * 根据微信交易号查询
     */
    Optional<WeChatPayment> findByTransactionId(String transactionId);
}
