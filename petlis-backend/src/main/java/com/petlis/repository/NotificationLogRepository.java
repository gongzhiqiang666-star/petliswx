package com.petlis.repository;

import com.petlis.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发送日志Repository
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Integer> {

    /**
     * 根据通知ID查询日志
     */
    List<NotificationLog> findByNotificationId(Integer notificationId);

    /**
     * 根据订单ID查询日志
     */
    List<NotificationLog> findByOrderId(Integer orderId);

    /**
     * 查询待发送的日志
     */
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.sendStatus = 0 AND nl.retryCount < 3")
    List<NotificationLog> findPendingLogs();

    /**
     * 查询发送失败的日志
     */
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.sendStatus = 2 AND nl.retryCount < 3 AND nl.sendTime > :since")
    List<NotificationLog> findFailedLogsSince(@Param("since") LocalDateTime since);

    /**
     * 统计发送成功次数
     */
    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.sendStatus = 1 AND nl.orderId = :orderId")
    Long countSuccessByOrderId(@Param("orderId") Integer orderId);

    /**
     * 统计发送失败次数
     */
    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.sendStatus = 2 AND nl.orderId = :orderId")
    Long countFailedByOrderId(@Param("orderId") Integer orderId);
}
