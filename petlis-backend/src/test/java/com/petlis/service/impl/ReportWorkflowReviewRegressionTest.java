package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportWorkflowReviewRegressionTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    LabWorkflowService laboratory;
    BusinessNotifications notifications;
    ReportWorkflowService service;

    @BeforeEach
    void setUp() {
        store = mock(FeatureStore.class);
        jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        access = mock(AccessService.class);
        laboratory = mock(LabWorkflowService.class);
        notifications = mock(BusinessNotifications.class);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.LAB_STAFF), Set.of(), Set.of(2),
                new DataScope(false, Set.of(), Set.of(2), null)));
        service = new ReportWorkflowService(store, access, laboratory, notifications, mock(ReportPdfGenerator.class), mock(PlatformFileService.class));
        lenient().when(store.one(contains("w.order_id,w.test_result_id"), anyLong())).thenReturn(Map.of("order_id", 8, "test_result_id", 31L));
        lenient().when(store.one(contains("SampleStatus"), anyLong())).thenReturn(Map.of("order_id", 8, "test_result_id", 31L, "SampleStatus", 2));
        lenient().when(store.one(startsWith("SELECT t.id,t.status"), eq(31L))).thenReturn(Map.of("status", "REPORT_REVIEW"));
    }

    Map<String, Object> report(long id, int detailId) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id); row.put("order_id", 8); row.put("detail_id", detailId); row.put("test_result_id", 31L);
        row.put("hospital_id", 1); row.put("laboratory_id", 2); row.put("status", "APPROVED"); row.put("doctor_user_id", 7);
        row.put("is_pay_money", 1); row.put("pay_type", 0); row.put("pdf_file_id", 51L); row.put("batch_no", "B-1");
        return row;
    }

    void completion(int total, int published) {
        when(store.one(contains("COUNT(DISTINCT"), eq(8))).thenReturn(Map.of("total", total, "published", published));
    }

    @Test
    void multiDetailOrderCompletesOnlyAfterEveryActiveDetailIsPublished() {
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report(41, 21));
        when(store.one(contains("SELECT w.*"), eq(42L))).thenReturn(report(42, 22));
        completion(2, 1);
        service.publish(41);
        verify(jdbc, never()).update(contains("SampleStatus=3"), any(Object[].class));
        verify(jdbc, never()).update(contains("lin_lab_task SET status='COMPLETED'"), any(Object[].class));
        verifyNoInteractions(notifications);

        completion(2, 2);
        service.publish(42);
        verify(jdbc).update(contains("SampleStatus=3"), eq(8));
        verify(jdbc).update(contains("lin_lab_task SET status='COMPLETED'"), any(Object[].class));
        verify(notifications).emitForOrder(eq(8), eq(7), eq("report"), eq("8:published"), any(), any(), any());
    }

    @Test
    void rejectingOneDetailDoesNotRegressSharedTaskAndNotifiesOnce() {
        var pending = report(41, 21); pending.put("status", "PENDING_REVIEW"); pending.put("submitted_by", 5);
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(pending);
        service.review(41, "REJECT", "结果不清晰");
        verify(jdbc, never()).update(contains("lin_lab_task"), any(Object[].class));
        verify(notifications, times(1)).emit(anyInt(), any(), any(), any(), any(), any());
    }

    @Test
    void repeatedPublishReturnsOriginalWithoutLegacyWritesOrNotification() {
        var published = report(41, 21); published.put("status", "PUBLISHED");
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(published);
        when(store.one(contains("SampleStatus"), anyLong())).thenReturn(Map.of("order_id", 8, "test_result_id", 31L, "SampleStatus", 3));
        when(store.one(startsWith("SELECT t.id,t.status"), eq(31L))).thenReturn(Map.of("status", "COMPLETED"));
        completion(1, 1);
        assertDoesNotThrow(() -> service.publish(41));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verifyNoInteractions(notifications);
    }

    @Test
    void publishedRetryRepairsMissingAggregateCompletion() {
        var published = report(41, 21); published.put("status", "PUBLISHED");
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(published);
        completion(1, 1);
        service.publish(41);
        verify(jdbc).update(contains("SampleStatus=3"), eq(8));
        verify(jdbc).update(contains("lin_lab_task SET status='COMPLETED'"), any(Object[].class));
        verify(notifications).emitForOrder(eq(8), eq(7), eq("report"), eq("8:published"), any(), any(), any());
    }

    @Test
    void sharedOrderAndTaskAreLockedBeforeWorkflow() {
        var approved = report(41, 21);
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(approved);
        completion(1, 1);
        service.publish(41);
        var order = org.mockito.Mockito.inOrder(store);
        order.verify(store).one(argThat(sql -> sql.contains("w.order_id,w.test_result_id") && sql.contains("SampleStatus") && sql.contains("FOR UPDATE OF o")), eq(41L));
        order.verify(store).one(startsWith("SELECT t.id,t.status"), eq(31L));
        order.verify(store).one(contains("SELECT w.*"), eq(41L));
    }

    @Test
    void firstWorkflowReadAndAggregateCountAreCurrentReadsBeforeAnyActorSnapshot() {
        var approved = report(41, 21);
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(approved);
        completion(1, 1);
        service.publish(41);
        var order = org.mockito.Mockito.inOrder(store, access);
        order.verify(store).one(argThat(sql -> sql.contains("w.order_id,w.test_result_id") && sql.contains("SampleStatus") && sql.contains("FOR UPDATE OF o")), eq(41L));
        order.verify(store).one(startsWith("SELECT t.id,t.status"), eq(31L));
        order.verify(access, atLeastOnce()).current();
        verify(store).one(argThat(sql -> sql.contains("COUNT(DISTINCT") && sql.contains("FOR UPDATE")), eq(8));
    }

    @Test
    void settlementSnapshotOverridesHospitalCurrentPayType() {
        var cash = report(41, 21); cash.put("is_pay_money", 0); cash.put("pay_type", 1); cash.put("settlement_mode", "cash");
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(cash);
        assertThrows(BusinessException.class, () -> service.publish(41));

        var monthly = report(41, 21); monthly.put("is_pay_money", 0); monthly.put("pay_type", 0); monthly.put("settlement_mode", "monthly");
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(monthly);
        completion(1, 1);
        assertDoesNotThrow(() -> service.publish(41));
        verify(jdbc).update(contains("SampleStatus=3"), eq(8));
    }

    @Test
    void publishStoresAbsoluteLegacyPdfUrlAndConfirmedCompleteStatusTwo() {
        var approved = report(41, 21);
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(approved);
        completion(1, 1);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "serverDomain", "https://laboklin.cn");
        service.publish(41);
        verify(jdbc).update(contains("DiagnosisStatus=2"), eq("https://laboklin.cn/Content/FilePdf/report-51.pdf"), eq(21));
    }

    @Test
    void legacyDetailBodyContainsReadableSummaryConclusionAndOnlyOwnResults() {
        var approved = report(41, 21);
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(approved);
        completion(1, 1);
        String snapshot = "{\"summary\":\"摘要第一行\\n摘要第二行\",\"conclusion\":\"结论\",\"items\":[{\"template\":\"模板\",\"name\":\"白细胞\",\"value\":\"12\",\"unit\":\"u\",\"referenceRange\":\"1-2\",\"abnormalFlag\":false}]}";
        when(jdbc.queryForList(contains("after_json"), eq("41"))).thenReturn(java.util.List.of(Map.of("after_json", snapshot)));
        service.publish(41);
        var content = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(contains("lin_lb_diagnosisdetail_info"), eq(21), content.capture(), any());
        assertTrue(content.getValue().startsWith("检验报告\n样本摘要：摘要第一行\n摘要第二行\n临床结论：结论"), content.getValue());
        assertTrue(content.getValue().contains("白细胞：12 u"));
        assertFalse(content.getValue().contains("{\"summary\""));
    }
}
