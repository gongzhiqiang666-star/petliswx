package com.petlis.repository;

import com.petlis.entity.business.NotificationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知记录Repository接口
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationRecord, Integer> {

    /**
     * 根据订单ID查询通知记录
     *
     * @param orderId 订单ID
     * @return 通知记录列表
     */
    @Query("SELECT n FROM NotificationRecord n WHERE n.orderId = :orderId ORDER BY n.operationTime DESC")
    List<NotificationRecord> findByOrderId(@Param("orderId") Integer orderId);

    /**
     * 根据医院ID查询通知记录(分页)
     *
     * @param hospitalId 医院ID
     * @param pageable   分页参数
     * @return 通知记录分页结果
     */
    @Query("SELECT n FROM NotificationRecord n WHERE n.hospitalId = :hospitalId ORDER BY n.operationTime DESC")
    Page<NotificationRecord> findByHospitalId(@Param("hospitalId") Integer hospitalId, Pageable pageable);

    /**
     * 查询待发送的微信通知
     *
     * @return 待发送的通知记录列表
     */
    @Query("SELECT n FROM NotificationRecord n WHERE n.wechatSentStatus = 0 ORDER BY n.operationTime ASC")
    List<NotificationRecord> findPendingNotifications();

    /**
     * 查询发送失败的通知记录
     *
     * @param beforeTime 时间阈值(查询此时间之前的失败记录)
     * @return 失败的通知记录列表
     */
    @Query("SELECT n FROM NotificationRecord n WHERE n.wechatSentStatus = 2 AND n.operationTime < :beforeTime ORDER BY n.operationTime DESC")
    List<NotificationRecord> findFailedNotifications(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计医院未读通知数量
     *
     * @param hospitalId 医院ID
     * @return 未读通知数量
     */
    @Query("SELECT COUNT(n) FROM NotificationRecord n WHERE n.hospitalId = :hospitalId AND n.wechatSentStatus = 0")
    Long countUnreadNotifications(@Param("hospitalId") Integer hospitalId);

    /**
     * 根据订单ID统计通知记录数量
     *
     * @param orderId 订单ID
     * @return 通知记录数量
     */
    @Query("SELECT COUNT(n) FROM NotificationRecord n WHERE n.orderId = :orderId")
    Long countByOrderId(@Param("orderId") Integer orderId);
}
