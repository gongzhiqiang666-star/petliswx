package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import com.petlis.config.WeChatPayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefundServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    AuditService audits;
    WeChatPayConfig config;
    RefundService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); access = mock(AccessService.class); audits = mock(AuditService.class); config = mock(WeChatPayConfig.class);
        when(store.jdbc()).thenReturn(jdbc); when(config.getMockEnabled()).thenReturn(true);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(), new DataScope(true, Set.of(), Set.of(), null)));
        service = new RefundService(store, access, audits, config);
    }

    Map<String, Object> paidOrder() {
        return Map.of("order_id", 8, "hospital_id", 7, "laboratory_id", 2, "settlement_mode", "cash", "SampleStatus", 3,
                "payment_no", "PAY-8", "amount", new BigDecimal("85.00"), "status", 1);
    }

    @Test void mockRefundIsIdempotentAndRevokesCashReportPaymentAccess() {
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(paidOrder());
        when(jdbc.queryForList(contains("MOCK_REFUND"), eq("8"))).thenReturn(List.of());
        when(jdbc.queryForObject(contains("lin_invoice_order"), eq(Boolean.class), eq(8))).thenReturn(false);

        var result = service.refund(8, "标本不合格", "refund-request-8");

        assertEquals("REFUNDED", result.get("status"));
        verify(jdbc).update(contains("lin_wechat_payment"), eq("PAY-8"));
        verify(jdbc).update(contains("IsPayMoney=0"), eq(8));
        verify(audits).record(eq("MOCK_REFUND_COMPLETED"), eq("MOCK_REFUND"), eq(8), any(), any(), eq("标本不合格"));
    }

    @Test void repeatedRefundReturnsOriginalWithoutAdditionalWrites() {
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(paidOrder());
        when(jdbc.queryForList(contains("MOCK_REFUND"), eq("8"))).thenReturn(List.of(Map.of("id", 3L, "resource_id", "8", "action", "MOCK_REFUND_COMPLETED")));
        var result = service.refund(8, "重复", "refund-request-8");
        assertEquals("REFUNDED", result.get("status"));
        verify(jdbc, never()).update(contains("lin_wechat_payment"), any(Object[].class));
    }

    @Test void refundRechecksAuditAfterOrderLock() {
        when(jdbc.queryForList(contains("MOCK_REFUND"), eq("8"))).thenReturn(List.of(), List.of(Map.of("id", 3L)));
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(paidOrder());
        service.refund(8, "并发重复", "refund-request-8");
        verify(jdbc, times(2)).queryForList(contains("MOCK_REFUND"), eq("8"));
        verify(jdbc, never()).update(contains("lin_wechat_payment"), any(Object[].class));
    }

    @Test void appliedOrIssuedInvoiceBlocksRefund() {
        when(jdbc.queryForList(contains("MOCK_REFUND"), eq("8"))).thenReturn(List.of());
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(paidOrder());
        when(jdbc.queryForObject(contains("lin_invoice_order"), eq(Boolean.class), eq(8))).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.refund(8, "安全边界", "refund-request-8"));
        verify(jdbc, never()).update(contains("IsPayMoney=0"), any(Object[].class));
    }

    @Test void foreignReplayIsDeniedBeforeAnyAuditOrRefundStateRead() {
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.HOSPITAL_ADMIN), Set.of(7), Set.of(), new DataScope(false, Set.of(7), Set.of(), null)));
        when(store.one(contains("ownership"), eq(8))).thenReturn(Map.of("order_id", 8, "hospital_id", 8, "laboratory_id", 2));
        assertThrows(BusinessException.class, () -> service.refund(8, "越权重放", "refund-request-8"));
        verify(jdbc, never()).queryForList(contains("MOCK_REFUND"), any(Object[].class));
    }

    @Test void monthlyBillAndRealPaymentAreNotRefundedByCashMockEndpoint() {
        when(jdbc.queryForList(contains("MOCK_REFUND"), eq("8"))).thenReturn(List.of());
        var monthly = new java.util.HashMap<>(paidOrder()); monthly.put("settlement_mode", "monthly");
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(monthly);
        assertThrows(BusinessException.class, () -> service.refund(8, "月结", "refund-request-8"));

        when(config.getMockEnabled()).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.refund(8, "真实支付", "refund-request-8"));
        verify(jdbc, never()).update(contains("lin_wechat_payment"), any(Object[].class));
    }
}
