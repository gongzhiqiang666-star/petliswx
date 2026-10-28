package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.ReviewPermission;
import com.petlis.service.OrderService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewServiceContractTest {
    @Test
    void pendingOrdersExposeLegacyYuanAmountAndSubmissionTime() {
        FeatureStore store = mock(FeatureStore.class);
        when(store.page(anyString(), anyString(), eq(0), eq(20))).thenReturn(Map.of(
                "items", List.of(Map.of("orderId", 9, "amount", "12.30", "submittedAt", "2026-10-27")),
                "total", 1, "page", 0, "size", 20));
        ReviewService service = new ReviewService(mock(BusinessNotifications.class), store,
                mock(ReviewPermission.class), new ObjectMapper(), mock(OrderService.class));

        var result = service.pendingOrders(0, 20);

        verify(store).page(contains("SampleStatus=0"), and(contains("p.TestFee AS amount"), contains("p.CreateDt AS submittedAt")), eq(0), eq(20));
        assert result.get("page").equals(0);
    }
}
