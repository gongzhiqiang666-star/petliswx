package com.petlis.service.impl;

import com.petlis.common.security.ReviewPermission;
import com.petlis.dto.request.OrderCreateRequest;
import com.petlis.mapper.OrderMapper;
import com.petlis.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplIdempotencyTest {
    @Mock BusinessNotifications notifications;
    @Mock DraftService drafts;
    @Mock FeatureStore store;
    @Mock ListDiagnosisProjectRepository projectRepository;
    @Mock DiagnosisDetailRepository detailRepository;
    @Mock DiagnosisDetailInfoRepository detailInfoRepository;
    @Mock InfoTestTypePriceRepository priceRepository;
    @Mock InfoTestTypeRepository infoTestTypeRepository;
    @Mock OrderMapper orderMapper;
    @Mock InfoHospitalRepository hospitalRepository;
    @Mock ReviewPermission reviewPermission;
    @Mock CouponService couponService;
    @Mock OrderWorkflowService orderWorkflows;
    @InjectMocks OrderServiceImpl service;

    @Test
    void completedReplayReturnsOriginalOrderBeforeDraftValidationOrPricing() {
        var request = new OrderCreateRequest();
        request.setIdempotencyKey("create-8");
        request.setDraftId(31L);
        request.setDraftVersion(2);
        when(orderWorkflows.begin(request)).thenReturn(OptionalInt.of(8));
        assertEquals(8, service.createOrder(request));
        verifyNoInteractions(drafts, couponService, projectRepository, notifications);
        verify(orderWorkflows, never()).complete(any(), anyInt(), anyInt());
    }
}
