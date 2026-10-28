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

import java.util.Optional;
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
    @Mock ContractService contracts;
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

    @Test
    void legacyMonthlyReviewWithoutContractUsesLegacyCredit() {
        monthlyProject();
        when(contracts.hasMonthlyContract(8)).thenReturn(false);
        service.updateOrderStatus(8, 2);
        verify(contracts, never()).reserveCredit(anyInt(), any());
        verify(projectRepository).save(any());
    }

    @Test
    void contractedMonthlyReviewStillChecksCredit() {
        monthlyProject();
        when(contracts.hasMonthlyContract(8)).thenReturn(true);
        service.updateOrderStatus(8, 2);
        verify(contracts).reserveCredit(eq(8), any());
    }

    @Test
    void nullEnglishOwnerNameIsStoredAsEmptyStringForLegacyNotNullColumn() {
        var request = new OrderCreateRequest();
        request.setMasterName("中文畜主");
        when(orderWorkflows.begin(request)).thenReturn(OptionalInt.empty());
        when(couponService.lockQuote(any(), any(), any())).thenReturn(new CouponService.Quote(1, java.util.Map.of(),
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null));
        var saved = new java.util.concurrent.atomic.AtomicReference<com.petlis.entity.business.ListDiagnosisProject>();
        when(projectRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            var project = invocation.getArgument(0, com.petlis.entity.business.ListDiagnosisProject.class);
            project.setListDiagnosisProjectId(8);
            saved.set(project);
            return project;
        });
        var hospital = new com.petlis.entity.dict.InfoHospital();
        hospital.setPayType(1);
        when(hospitalRepository.findById(1)).thenReturn(Optional.of(hospital));
        try (var security = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            security.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId).thenReturn(1);
            security.when(com.petlis.common.security.SecurityUtils::getCurrentCityId).thenReturn(2);
            security.when(com.petlis.common.security.SecurityUtils::getCurrentUserId).thenReturn(7);
            assertEquals(8, service.createOrder(request));
        }
        assertEquals("", saved.get().getMasterNameEn());
    }

    private void monthlyProject() {
        var project = new com.petlis.entity.business.ListDiagnosisProject();
        project.setListDiagnosisProjectId(8);
        project.setInfoHospitalId(1);
        project.setSampleStatus(0);
        project.setTestFee(75.0);
        project.setCreateSampleId(7);
        when(projectRepository.findLockedById(8)).thenReturn(Optional.of(project));
        when(reviewPermission.isReviewer()).thenReturn(true);
        var hospital = new com.petlis.entity.dict.InfoHospital();
        hospital.setPayType(1);
        when(hospitalRepository.findById(1)).thenReturn(Optional.of(hospital));
        when(couponService.isMonthly(8, true)).thenReturn(true);
    }
}
