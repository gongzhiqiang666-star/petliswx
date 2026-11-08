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
    void assignmentReturnsThePendingRetestRatherThanHistoricalInitialTask() {
        when(store.one(contains("lin_shipment"), eq(8))).thenReturn(Map.of("laboratory_id", 2));
        when(jdbc.queryForObject(contains("lin_user_role"), eq(Boolean.class), eq(5), eq(2))).thenReturn(true);
        when(store.one(contains("SELECT * FROM lin_lab_task"), eq(8), eq(2))).thenReturn(Map.of("id", 12L));
        when(jdbc.update(contains("WHERE id=? AND status='RECEIVED'"), eq(5), eq("PCR-2"), eq(12L))).thenReturn(1);
        var child = new java.util.HashMap<String,Object>(task("TESTING"));
        child.put("id", 12L); child.put("task_type", "RETEST");
        when(store.one(contains("WHERE t.id=?"), eq(12L))).thenReturn(child);
        assertEquals(12L, ((Number)service.assign(8, 5, "PCR-2").get("id")).longValue());
        verify(notifications).emit(eq(5), eq("order"), eq("lab-task:12:ASSIGNED"), anyString(), anyString(), anyString());
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
    @Test void reassignPreservesStartTimeAndAuditsBeforeAndAfter() throws Exception {
        var before=new java.util.LinkedHashMap<String,Object>(task("TESTING"));before.put("version",3);
        when(store.one(contains("WHERE t.id=?"),eq(11L))).thenReturn(before);
        when(jdbc.queryForObject(contains("lin_user_role"),eq(Boolean.class),eq(6),eq(2))).thenReturn(true);
        when(jdbc.update(contains("WHERE id=? AND version=?"),eq(6),eq("PCR-2"),eq(11L),eq(3))).thenReturn(1);
        when(store.encode(any())).thenAnswer(call->new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(call.getArgument(0)));
        var result=service.reassign(11,6,"PCR-2",3,"人员交接");
        assertEquals(6,result.get("assigned_to"));assertEquals(4,result.get("version"));
        verify(store).insert(contains("before_json,after_json"),eq(5),eq("LAB_TASK_REASSIGNED"),eq("11"),contains("\"assignedTo\":5"),contains("\"assignedTo\":6"),eq("人员交接"));
        verify(notifications).emit(eq(6),eq("order"),eq("lab-task:11:REASSIGNED:4:new"),anyString(),anyString(),anyString());
        verify(notifications).emit(eq(5),eq("order"),eq("lab-task:11:REASSIGNED:4:old"),anyString(),anyString(),anyString());
    }
    @Test void reassignRejectsStaleVersionRecordedResultAndForeignAssignee() {
        var before=new java.util.LinkedHashMap<String,Object>(task("TESTING"));before.put("version",3);
        when(store.one(contains("WHERE t.id=?"),eq(11L))).thenReturn(before);
        assertThrows(BusinessException.class,()->service.reassign(11,6,"PCR-2",2,"交接"));
        assertThrows(BusinessException.class,()->service.reassign(11,6,"PCR-2",3,"交接"));
        when(jdbc.queryForObject(contains("lin_user_role"),eq(Boolean.class),eq(6),eq(2))).thenReturn(true);
        when(jdbc.queryForObject(contains("lin_test_result"),eq(Boolean.class),eq(11L))).thenReturn(true);
        assertThrows(BusinessException.class,()->service.reassign(11,6,"PCR-2",3,"交接"));
        before.put("status","RESULT_ENTERED");assertThrows(BusinessException.class,()->service.reassign(11,6,"PCR-2",3,"交接"));
        verify(jdbc,never()).update(contains("assigned_to"),any(Object[].class));verifyNoInteractions(notifications);
    }
    @Test void unchangedAssignmentIsReadOnly() {
        var before=new java.util.LinkedHashMap<String,Object>(task("TESTING"));before.put("version",3);
        when(store.one(contains("WHERE t.id=?"),eq(11L))).thenReturn(before);
        when(jdbc.queryForObject(contains("lin_user_role"),eq(Boolean.class),eq(5),eq(2))).thenReturn(true);
        service.reassign(11,5,"PCR-1",3,"核对未调整");
        verify(jdbc,never()).update(contains("assigned_to"),any(Object[].class));verify(store,never()).insert(anyString(),any(Object[].class));verifyNoInteractions(notifications);
    }
}
