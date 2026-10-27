package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.ReviewPermission;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkflowNotificationFallbackTest {
    @Test
    void missingOrderCreatorFallsBackToHospitalRecipientsWithSameBusinessKey() {
        FeatureStore store = mock(FeatureStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        when(jdbc.queryForList(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(List.of(Map.of("id", 9)));
        var notifications = new BusinessNotifications(store, mock(SubscriptionPush.class), mock(ReviewPermission.class),
                mock(TransactionTemplate.class), mock(Environment.class), new ObjectMapper());
        notifications.emitForOrder(8, 0, "report", "report-workflow:41:PUBLISHED", "报告已发布", "内容", "pages/report-detail/report-detail?id=21");
        verify(jdbc).update(contains("ON DUPLICATE KEY UPDATE id=id"), eq(9), eq("report"), eq("report-workflow:41:PUBLISHED"), any(), any(), any());
    }
}
