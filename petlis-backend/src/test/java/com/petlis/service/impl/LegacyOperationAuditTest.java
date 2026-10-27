package com.petlis.service.impl;

import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LegacyOperationAuditTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    ReviewPermission permission;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); permission = mock(ReviewPermission.class);
        when(store.jdbc()).thenReturn(jdbc); when(store.encode(any())).thenReturn("{}");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(5, null, 7, 1), null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void articlePublishKeepsSingleOutboxAndWritesAudit() {
        BusinessNotifications notifications = mock(BusinessNotifications.class);
        when(store.insert(contains("lin_article"), any(Object[].class))).thenReturn(41L);
        ArticleService service = new ArticleService(notifications, store, permission);
        long id = service.publish(new ArticleService.Article("新闻", "article", "标题", "摘要", "正文", "", null, null, false));
        verify(notifications).article(eq(id), any(), any());
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("ARTICLE_PUBLISHED"), eq("ARTICLE"), eq("41"));
    }

    @Test void articleOutboxUpsertDoesNotUseAmbiguousIdReference() {
        BusinessNotifications service = new BusinessNotifications(store, mock(SubscriptionPush.class), permission,
                mock(TransactionTemplate.class), new MockEnvironment(), new ObjectMapper());
        service.article(41, "标题", "摘要");
        verify(jdbc).update(contains("ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(lin_notification_event.id)"), any(Object[].class));
    }

    @Test void invoiceIssueDoesNotRewriteImmutableProfileSnapshotAndAudits() {
        BusinessNotifications notifications = mock(BusinessNotifications.class);
        when(store.one(contains("lin_invoice_application"), eq(31L))).thenReturn(Map.of("status", "pending", "user_id", 5));
        InvoiceService service = new InvoiceService(notifications, store, permission, new ObjectMapper());
        service.issue(31, "https://files.example/invoice.pdf");
        verify(jdbc).update(contains("status='issued'"), eq("https://files.example/invoice.pdf"), eq(31L));
        verify(jdbc, never()).update(contains("profile_snapshot="), any(Object[].class));
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("INVOICE_ISSUED"), eq("INVOICE_APPLICATION"), eq("31"));
    }

    @Test void invoiceApplicationAndRejectionWriteAudits() {
        BusinessNotifications notifications = mock(BusinessNotifications.class);
        InvoiceService service = new InvoiceService(notifications, store, permission, new ObjectMapper());
        when(store.one(contains("lin_invoice_profile"), eq(2L), eq(5))).thenReturn(new java.util.HashMap<>(Map.of("title", "医院", "email", "a@example.com")));
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8), eq(5), eq(7))).thenReturn(Map.of("TestFee", 10.0, "IsPayMoney", 1));
        when(store.insert(contains("lin_invoice_application"), any(Object[].class))).thenReturn(31L);
        service.apply(new InvoiceService.Application(2, List.of(8), "invoice-request-1"));
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("INVOICE_APPLIED"), eq("INVOICE_APPLICATION"), eq("31"), eq("invoice-request-1"));

        when(store.one(contains("lin_invoice_application"), eq(31L))).thenReturn(Map.of("status", "pending", "user_id", 5));
        service.reject(31, "信息有误");
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("INVOICE_REJECTED"), eq("INVOICE_APPLICATION"), eq("31"), eq("信息有误"));
    }

    @Test void giftRedemptionWritesAuditOnExistingMasters() {
        when(store.one(contains("lin_user"), eq(5))).thenReturn(Map.of("score", 100));
        when(store.one(contains("lin_gift"), eq(9L))).thenReturn(Map.of("id", 9L, "name", "礼品", "points_cost", 10, "stock", 5, "active", 1));
        when(store.insert(contains("lin_redemption"), any(Object[].class))).thenReturn(31L);
        MallService service = new MallService(mock(BusinessNotifications.class), store, permission, new ObjectMapper());
        service.redeem(new MallService.Redemption(9, 1, new MallService.Address("医生", "19900000000", "地址"), "redemption-key-1"));
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("REDEMPTION_CREATED"), eq("REDEMPTION"), eq("31"), eq("redemption-key-1"));
    }

    @Test void legacyNotificationRetryAlsoWritesAudit() {
        var repository = mock(com.petlis.repository.NotificationRepository.class);
        var push = mock(com.petlis.service.WechatNotificationService.class);
        var record = new com.petlis.entity.business.NotificationRecord(); record.setId(61); record.setWechatSentStatus(2);
        when(repository.findById(61)).thenReturn(java.util.Optional.of(record)); when(push.sendOrderStatusNotification(record)).thenReturn(true);
        NotificationServiceImpl service = new NotificationServiceImpl(repository, jdbc, permission, mock(com.petlis.repository.InfoHospitalRepository.class),
                mock(com.petlis.repository.UserRepository.class), mock(com.petlis.mapper.NotificationMapper.class), push);
        service.retryNotification(61);
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("LEGACY_NOTIFICATION_RETRIED"), eq("NOTIFICATION"), eq("61"));
    }

    @Test void messageRetryKeepsOldOutboxAndWritesAudit() {
        when(permission.isReviewer()).thenReturn(true);
        when(store.one(contains("lin_notification_event"), eq(51L))).thenReturn(Map.of("id", 51L));
        when(jdbc.update(contains("push_status='pending'"), eq(51L))).thenReturn(1);
        BusinessNotifications service = new BusinessNotifications(store, mock(SubscriptionPush.class), permission,
                mock(TransactionTemplate.class), new MockEnvironment(), new ObjectMapper());
        service.retry(51);
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("NOTIFICATION_RETRIED"), eq("NOTIFICATION_EVENT"), eq("51"));
    }
}
