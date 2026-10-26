package com.petlis.scheduler;

import com.petlis.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 通知定时任务
 * 定时扫描待发送的微信通知并发送
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.notifications.scheduler-enabled", havingValue="true")
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;

    /**
     * 处理待发送的微信通知
     * 每隔1分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒
    public void processPendingNotifications() {
        try {
            log.debug("[定时任务] 开始扫描待发送通知");

            Integer count = notificationService.processPendingNotifications();

            if (count > 0) {
                log.info("[定时任务] 处理了 {} 条待发送通知", count);
            }
        } catch (Exception e) {
            log.error("[定时任务] 处理待发送通知失败", e);
        }
    }

    /**
     * 清理过期的通知记录（可选）
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldNotifications() {
        try {
            log.info("[定时任务] 开始清理过期通知记录");
            // TODO: 实现30天前的通知记录清理逻辑
            // notificationService.deleteOldNotifications(30);
        } catch (Exception e) {
            log.error("[定时任务] 清理过期通知失败", e);
        }
    }
}
