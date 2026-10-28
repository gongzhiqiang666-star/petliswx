package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportWorkflowServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    LabWorkflowService laboratory;
    BusinessNotifications notifications;
    ReportPdfGenerator pdf;
    PlatformFileService files;
    ReportWorkflowService service;

    @BeforeEach
    void setUp() {
        store = mock(FeatureStore.class);
        jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        access = mock(AccessService.class);
        laboratory = mock(LabWorkflowService.class);
        notifications = mock(BusinessNotifications.class);
        pdf = mock(ReportPdfGenerator.class);
        files = mock(PlatformFileService.class);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.LAB_STAFF), Set.of(), Set.of(2),
                new DataScope(false, Set.of(), Set.of(2), null)));
        service = new ReportWorkflowService(store, access, laboratory, notifications, pdf, files);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "serverDomain", "https://laboklin.cn");
        lenient().when(store.one(contains("COUNT(DISTINCT"), eq(8))).thenReturn(Map.of("total", 1, "published", 1));
        lenient().when(store.one(contains("w.order_id,w.test_result_id"), eq(41L))).thenReturn(Map.of("order_id", 8, "test_result_id", 31L));
        lenient().when(store.one(contains("SampleStatus"), anyLong())).thenReturn(Map.of("order_id", 8, "test_result_id", 31L, "SampleStatus", 2));
        lenient().when(store.one(startsWith("SELECT t.id,t.status"), eq(31L))).thenReturn(Map.of("status", "REPORT_REVIEW"));
    }

    Map<String, Object> report(String status, int paid, int payType) {
        return Map.ofEntries(Map.entry("id", 41L), Map.entry("order_id", 8), Map.entry("detail_id", 21), Map.entry("test_result_id", 31L),
                Map.entry("hospital_id", 1), Map.entry("laboratory_id", 2), Map.entry("status", status), Map.entry("doctor_user_id", 7),
                Map.entry("is_pay_money", paid), Map.entry("pay_type", payType), Map.entry("pdf_file_id", 51L), Map.entry("batch_no", "B-1"));
    }

    @Test
    void pendingReviewCannotBePublished() {
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report("PENDING_REVIEW", 1, 0));
        assertThrows(BusinessException.class, () -> service.publish(41));
        verify(jdbc, never()).update(contains("SampleStatus=3"), any(Object[].class));
    }

    @Test
    void approvedReportWithVoidedResultCannotPublish() {
        var workflow = new java.util.HashMap<>(report("APPROVED", 1, 0));
        workflow.put("result_status", "VOIDED");
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(workflow);
        assertThrows(BusinessException.class, () -> service.publish(41));
        verify(jdbc, never()).update(contains("SampleStatus=3"), any(Object[].class));
    }

    @Test
    void approvedCashReportRequiresPaymentBeforeAnyLegacyWrite() {
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report("APPROVED", 0, 0));
        assertThrows(BusinessException.class, () -> service.publish(41));
        verify(jdbc, never()).update(contains("DiagnosisStatus=2"), any(Object[].class));
    }

    @Test
    void publishSynchronizesLegacyDetailInfoPdfAndOrderAndEmitsStableEvent() {
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report("APPROVED", 1, 0));
        var published = service.publish(41);
        assertEquals("PUBLISHED", published.get("status"));
        verify(jdbc).update(contains("DiagnosisStatus=2"), eq("/api/pdf/view/21"), eq(21));
        verify(jdbc).update(contains("lin_lb_diagnosisdetail_info"), eq(21), any(), any());
        verify(jdbc).update(contains("SampleStatus=3"), eq(8));
        verify(notifications).emitForOrder(eq(8), eq(7), eq("report"), eq("8:published"), any(), any(), any());
    }

    @Test
    void monthlyReportCanPublishWithoutImmediatePayment() {
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report("APPROVED", 0, 1));
        assertDoesNotThrow(() -> service.publish(41));
        verify(jdbc).update(contains("SampleStatus=3"), eq(8));
    }

    @Test
    void repeatedPublishReturnsOriginalWithoutDuplicateNotification() {
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report("APPROVED", 1, 0), report("PUBLISHED", 1, 0));
        when(store.one(contains("SampleStatus"), anyLong())).thenReturn(Map.of("order_id", 8, "test_result_id", 31L, "SampleStatus", 2), Map.of("order_id", 8, "test_result_id", 31L, "SampleStatus", 3));
        when(store.one(startsWith("SELECT t.id,t.status"), eq(31L))).thenReturn(Map.of("status", "REPORT_REVIEW"), Map.of("status", "COMPLETED"));
        service.publish(41);
        assertDoesNotThrow(() -> service.publish(41));
        verify(notifications, times(1)).emitForOrder(eq(8), eq(7), eq("report"), eq("8:published"), any(), any(), any());
    }

    @Test
    void publishedCashReportRemainsInvisibleUntilPaid() {
        when(access.current()).thenReturn(new CurrentActor(7, Set.of(PlatformRole.DOCTOR), Set.of(1), Set.of(),
                new DataScope(false, Set.of(1), Set.of(), null)));
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report("PUBLISHED", 0, 0));
        assertThrows(BusinessException.class, () -> service.get(41));
    }

    @Test
    void hospitalCannotReadUnpublishedOrForeignWorkflow() {
        when(access.current()).thenReturn(new CurrentActor(7, Set.of(PlatformRole.DOCTOR), Set.of(1), Set.of(),
                new DataScope(false, Set.of(1), Set.of(), null)));
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report("APPROVED", 1, 0));
        assertThrows(BusinessException.class, () -> service.get(41));
        verify(notifications, never()).emit(anyInt(), any(), any(), any(), any(), any());
    }

    @Test
    void generationRequiresCompleteCurrentBatchAndUsesTestTypeTemplates() {
        when(laboratory.taskForReport(11)).thenReturn(Map.of("id", 11L, "order_id", 8, "laboratory_id", 2,
                "hospital_id", 1, "status", "RESULT_ENTERED", "doctor_user_id", 7));
        when(laboratory.activeResults(11)).thenReturn(java.util.List.of(Map.of("id", 31L, "batch_no", "B-1", "result_items", "[{\"orderItemId\":21,\"name\":\"白细胞\",\"value\":\"12\",\"unit\":\"10^9/L\",\"referenceRange\":\"5-10\",\"abnormalFlag\":true}]")));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail"), eq(8))).thenReturn(java.util.List.of(
                Map.of("detail_id", 21, "info_test_type_id", 91)));
        when(jdbc.queryForList(contains("lin_report_template"), eq(2), eq(91))).thenReturn(java.util.List.of(
                Map.of("id", 61L, "version", 3, "content", "血常规模板")));
        when(jdbc.queryForObject(contains("COUNT(*)"), eq(Long.class), eq(8))).thenReturn(1L);
        when(store.insert(contains("lin_report_workflow"), any(Object[].class))).thenReturn(41L);
        when(pdf.create(any(), any(), any(), any(), any())).thenReturn(new byte[]{1});
        when(files.saveReport(anyInt(), anyInt(), anyLong(), any())).thenReturn(new PlatformFileService.Stored(51L, "/Content/FilePdf/report-51.pdf"));

        var ids = service.generate(11, "摘要", "结论");
        assertEquals(java.util.List.of(41L), ids);
        verify(jdbc).queryForList(contains("lin_report_template"), eq(2), eq(91));
        verify(pdf).create(any(), eq("摘要"), eq("结论"), any(), any());
        var invocation = mockingDetails(store).getInvocations().stream().filter(call -> call.getMethod().getName().equals("insert")
                && call.getArgument(0).toString().contains("lin_report_workflow")).findFirst().orElseThrow();
        String insertSql = invocation.getArgument(0);
        int argumentCount = invocation.getArguments().length - 1;
        assertTrue(insertSql.contains("template_id"));
        assertEquals(insertSql.chars().filter(character -> character == '?').count(), argumentCount);
        verify(jdbc, never()).update(contains("template_version=1"), any(Object[].class));
    }

    @Test
    void activeWorkflowCannotBeGeneratedTwiceForTheSameOrder() {
        when(laboratory.taskForReport(11)).thenReturn(Map.of("id", 11L, "order_id", 8, "laboratory_id", 2,
                "hospital_id", 1, "status", "RESULT_ENTERED", "doctor_user_id", 7));
        when(laboratory.activeResults(11)).thenReturn(java.util.List.of(Map.of("id", 31L, "batch_no", "B-1", "result_items", "[{\"orderItemId\":21,\"name\":\"白细胞\",\"value\":\"12\",\"unit\":\"10^9/L\",\"referenceRange\":\"5-10\",\"abnormalFlag\":true}]")));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail"), eq(8))).thenReturn(java.util.List.of(Map.of("detail_id", 21, "info_test_type_id", 91)));
        when(jdbc.queryForObject(contains("COUNT(*)"), eq(Long.class), eq(8))).thenReturn(1L);
        when(jdbc.queryForObject(contains("EXISTS(SELECT 1 FROM lin_report_workflow"), eq(Boolean.class), eq(8))).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.generate(11, "摘要", "结论"));
        verify(store, never()).insert(contains("lin_report_workflow"), any(Object[].class));
    }

    @Test
    void hospitalWorkflowListExcludesUnpaidCashReports() {
        when(access.current()).thenReturn(new CurrentActor(7, Set.of(PlatformRole.DOCTOR), Set.of(1), Set.of(),
                new DataScope(false, Set.of(1), Set.of(), null)));
        service.list(null, 0, 20);
        verify(store).page(contains("h.PayType=1"), contains("h.PayType=1"), eq(0), eq(20), any(Object[].class));
    }
}
