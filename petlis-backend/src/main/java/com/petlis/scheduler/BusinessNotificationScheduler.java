package com.petlis.scheduler;
import com.petlis.service.impl.BusinessNotifications;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor @ConditionalOnProperty(name="app.notifications.events-enabled",havingValue="true")
public class BusinessNotificationScheduler {
 private final BusinessNotifications notifications;
 @Scheduled(fixedDelayString="${app.notifications.interval-ms:30000}") public void process(){notifications.process(20);}
}
