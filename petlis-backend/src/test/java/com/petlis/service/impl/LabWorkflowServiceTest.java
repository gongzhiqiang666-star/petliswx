package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LabWorkflowServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    BusinessNotifications notifications;
    LabWorkflowService service;

    @BeforeEach
    void setUp() {
        store = mock(FeatureStore.class);
        jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        access = mock(AccessService.class);
        notifications = mock(BusinessNotifications.class);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.LAB_STAFF),
                Set.of(), Set.of(2), new DataScope(false, Set.of(), Set.of(2), null)));
        service = new LabWorkflowService(store, access, notifications);
    }

    Map<String, Object> task(String status) {
        return Map.of("id", 11L, "order_id", 8, "laboratory_id", 2, "status", status,
                "assigned_to", 5, "equipment_code", "PCR-1", "batch_no", "B-1", "doctor_user_id", 7);
    }

    @Test
    void assigneesUseLegacyUsernameColumn() {
        when(access.current()).thenReturn(new CurrentActor(1, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(),
                new DataScope(true, Set.of(), Set.of(), null)));
        when(jdbc.queryForList(anyString(), eq(2))).thenReturn(List.of());
        service.assignees(2);
        verify(jdbc).queryForList(contains("u.username"), eq(2));
        verify(jdbc, never()).queryForList(contains("u.account"), eq(2));
    }

    @Test
    void assigneeMustHaveActiveRoleInTheSameLaboratory() {
        when(store.one(contains("lin_shipment"), eq(8))).thenReturn(Map.of(
                "id", 11L, "order_id", 8, "laboratory_id", 2, "status", "RECEIVED", "doctor_user_id", 7));
        when(jdbc.queryForObject(contains("lin_user_role"), eq(Boolean.class), eq(6), eq(2))).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.assign(8, 6, "PCR-1"));
        verify(jdbc, never()).update(contains("lin_lab_task"), any(Object[].class));
    }

    @Test
    void resultsMustCoverEveryActiveDetailAndContainCompleteFields() {
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("TESTING"));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail"), eq(Integer.class), eq(8))).thenReturn(List.of(21, 22));
        var onlyOne = new LabWorkflowService.ResultsCommand(List.of(
                new LabWorkflowService.ResultCommand(21, "白细胞", "6.2", "10^9/L", "5-10", null)));
        assertThrows(BusinessException.class, () -> service.enterResults(11, onlyOne));
        verify(store, never()).insert(contains("lin_test_result"), any(Object[].class));
    }

    @Test
    void qualitativeResultRequiresExplicitAbnormalFlag() {
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("TESTING"));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail"), eq(Integer.class), eq(8))).thenReturn(List.of(21));
        var input = new LabWorkflowService.ResultsCommand(List.of(
                new LabWorkflowService.ResultCommand(21, "抗体", "阳性", "", "阴性", null)));
        assertThrows(BusinessException.class, () -> service.enterResults(11, input));
    }

    @Test
    void numericRangeDerivesAbnormalAndStoresOneImmutableBatch() {
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("TESTING"));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail"), eq(Integer.class), eq(8))).thenReturn(List.of(21));
        when(store.insert(contains("lin_test_result"), any(Object[].class))).thenReturn(31L);
        var input = new LabWorkflowService.ResultsCommand(List.of(
                new LabWorkflowService.ResultCommand(21, "白细胞", "12", "10^9/L", "5-10", null)));
        service.enterResults(11, input);
        verify(store).insert(contains("lin_test_result"), any(Object[].class));
        verify(jdbc, never()).update(contains("UPDATE lin_test_result SET result_items"), any(Object[].class));
    }

    @Test
    void retestVoidsOldResultsAndRequiresANewTaskAndBatch() {
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("RETURNED"));
        when(store.insert(contains("lin_lab_task"), any(Object[].class))).thenReturn(12L);
        var created = service.retest(11, "样本凝集");
        assertEquals(12L, created);
        verify(jdbc).update(contains("result_status='VOIDED'"), eq(5), eq(11L));
        verify(store).insert(contains("return_reason"), any(Object[].class));
    }

    @Test
    void returningSampleVoidsResultsAndUnpublishedReports() {
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("RESULT_ENTERED"));
        service.returnSample(11, "样本不足");
        verify(jdbc).update(contains("result_status='VOIDED'"), eq(5), eq(11L));
        verify(jdbc).update(contains("status='CANCELLED'"), eq(11L));
    }

    @Test
    void reportTaskLookupLocksTheTaskAgainstConcurrentGeneration() {
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("RESULT_ENTERED"));
        service.taskForReport(11);
        verify(store).one(contains("FOR UPDATE"), eq(11L));
    }

    @Test
    void onlyAssignedPersonCanEnterResults() {
        when(access.current()).thenReturn(new CurrentActor(6, Set.of(PlatformRole.LAB_STAFF), Set.of(), Set.of(2),
                new DataScope(false, Set.of(), Set.of(2), null)));
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("TESTING"));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail"), eq(Integer.class), eq(8))).thenReturn(List.of(21));
        var input = new LabWorkflowService.ResultsCommand(List.of(new LabWorkflowService.ResultCommand(21, "白细胞", "6", "10^9/L", "5-10", null)));
        BusinessException error = assertThrows(BusinessException.class, () -> service.enterResults(11, input));
        assertEquals(403, error.getCode());
        verify(store, never()).insert(contains("lin_test_result"), any(Object[].class));
    }

    @Test
    void repeatedRetestReturnsExistingChildAndChangedReasonConflicts() {
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("RETESTED"));
        when(store.one(contains("parent_task_id"), eq(11L))).thenReturn(Map.of("id", 12L, "return_reason", "样本凝集"));
        assertEquals(12L, service.retest(11, "样本凝集"));
        assertThrows(BusinessException.class, () -> service.retest(11, "不同原因"));
        verify(store, never()).insert(contains("lin_lab_task"), any(Object[].class));
        verifyNoInteractions(notifications);
    }

    @Test
    void returnedTaskCannotOverwriteItsHistoricalResults() {
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task("RETURNED"));
        assertThrows(BusinessException.class, () -> service.enterResults(11,
                new LabWorkflowService.ResultsCommand(List.of())));
        verify(store, never()).insert(contains("lin_test_result"), any(Object[].class));
    }
}
