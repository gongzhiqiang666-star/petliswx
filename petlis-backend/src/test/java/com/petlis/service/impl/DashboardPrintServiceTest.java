package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
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

class DashboardPrintServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    AuditService audits;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); access = mock(AccessService.class); audits = mock(AuditService.class);
        when(store.jdbc()).thenReturn(jdbc);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(), new DataScope(true, Set.of(), Set.of(), null)));
    }

    @Test void dashboardAggregatesOrdersCashAndSettledMonthlyWithoutWalletDoubleCount() {
        DashboardService service = new DashboardService(store, access);
        when(store.one(contains("lin_wechat_payment"), any(Object[].class))).thenReturn(Map.of("orderCount", 4L, "cashRevenue", new BigDecimal("100.00"), "monthlyRevenue", new BigDecimal("80.00"), "arrears", new BigDecimal("20.00")));
        when(jdbc.queryForList(contains("HospitalName"), any(Object[].class))).thenReturn(List.of(Map.of("hospitalName", "一院", "revenue", new BigDecimal("180.00"), "orders", 4L)));
        when(jdbc.queryForList(contains("lin_lb_infotesttype"), any(Object[].class))).thenReturn(List.of(Map.of("testName", "血常规", "quantity", 4L)));
        var result = service.dashboard("2026-01-01", "2026-02-01");
        assertEquals(new BigDecimal("180.00"), result.get("revenue"));
        assertEquals(4L, result.get("orderCount"));
        assertFalse(result.containsKey("walletRevenue"));
        verify(jdbc).queryForList(contains("p.paid_at>=?"), any(Object[].class));
    }

    @Test void csvExportEscapesFormulaAndUsesLegacyOrderIds() {
        DashboardService service = new DashboardService(store, access);
        when(jdbc.queryForList(contains("LIMIT 10000"), any(Object[].class))).thenReturn(List.of(Map.of("orderId", 8, "orderNo", "=BAD", "hospitalName", "一院", "amount", new BigDecimal("10.00"), "status", 3)));
        String csv = service.exportOrders("2026-01-01", "2026-02-01");
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("\"'=BAD\""));
        assertTrue(csv.contains("8"));
    }

    @Test void printViewsContainChineseOldIdsAndAuditedBarcode() {
        PrintService service = new PrintService(store, access, audits);
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(Map.of("orderId", 8, "orderNo", "55550-8", "hospitalId", 7, "laboratoryId", 2, "hospitalName", "宠物医院", "animalName", "豆豆", "amount", new BigDecimal("10.00")));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail"), eq(8))).thenReturn(List.of(Map.of("detailId", 81, "testName", "血常规", "price", new BigDecimal("10.00"))));
        String html = service.order(8);
        String label = service.specimenLabel(8);
        assertTrue(html.contains("55550-8"));
        assertTrue(html.contains("宠物医院"));
        assertTrue(label.contains("svg"));
        assertTrue(label.contains("55550-8"));
        verify(audits, times(2)).record(startsWith("PRINT_"), any(), eq(8), any(), any(), any());
    }

    @Test void unpublishedReportCannotBePrintedEvenWhenPaid() {
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.HOSPITAL_ADMIN), Set.of(7), Set.of(), new DataScope(false, Set.of(7), Set.of(), null)));
        PrintService service = new PrintService(store, access, audits);
        when(store.one(contains("lin_lb_diagnosisdetail_info"), eq(81))).thenReturn(Map.of("detailId", 81, "orderId", 8, "hospitalId", 7, "laboratoryId", 2, "settlementMode", "cash", "paid", 1, "sampleStatus", 2, "result", "草稿"));
        assertThrows(BusinessException.class, () -> service.report(81));
        verify(audits, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test void unpaidCashReportPrintIsDeniedAndForeignHospitalIsRejected() {
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.HOSPITAL_ADMIN), Set.of(7), Set.of(), new DataScope(false, Set.of(7), Set.of(), null)));
        PrintService service = new PrintService(store, access, audits);
        when(store.one(contains("lin_lb_diagnosisdetail_info"), eq(81))).thenReturn(Map.of("detailId", 81, "orderId", 8, "hospitalId", 8, "laboratoryId", 2, "settlementMode", "cash", "paid", 0, "result", "结果"));
        assertThrows(BusinessException.class, () -> service.report(81));
        verify(audits, never()).record(any(), any(), any(), any(), any(), any());
    }
}
