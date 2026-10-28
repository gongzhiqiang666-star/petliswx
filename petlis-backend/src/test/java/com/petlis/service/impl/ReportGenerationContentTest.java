package com.petlis.service.impl;

import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportGenerationContentTest {
    @Test
    void eachDetailPdfAndSnapshotContainsOnlyItsOwnProjectResult() {
        FeatureStore store = mock(FeatureStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        AccessService access = mock(AccessService.class);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.LAB_STAFF), Set.of(), Set.of(2), new DataScope(false, Set.of(), Set.of(2), null)));
        LabWorkflowService laboratory = mock(LabWorkflowService.class);
        ReportPdfGenerator pdf = mock(ReportPdfGenerator.class);
        PlatformFileService files = mock(PlatformFileService.class);
        when(laboratory.taskForReport(11)).thenReturn(Map.of("id", 11L, "order_id", 8, "laboratory_id", 2, "hospital_id", 1, "status", "REPORT_REVIEW", "doctor_user_id", 7));
        String results = "[{\"orderItemId\":21,\"name\":\"项目一\",\"value\":\"1\",\"unit\":\"u\",\"referenceRange\":\"0-2\",\"abnormalFlag\":false},{\"orderItemId\":22,\"name\":\"项目二\",\"value\":\"2\",\"unit\":\"u\",\"referenceRange\":\"0-3\",\"abnormalFlag\":false}]";
        when(laboratory.activeResults(11)).thenReturn(List.of(Map.of("id", 31L, "batch_no", "B-1", "result_items", results)));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail"), eq(8))).thenReturn(List.of(Map.of("detail_id", 21, "info_test_type_id", 91), Map.of("detail_id", 22, "info_test_type_id", 92)));
        when(jdbc.queryForObject(contains("COUNT(*)"), eq(Long.class), eq(8))).thenReturn(2L);
        when(store.insert(contains("lin_report_workflow"), any(Object[].class))).thenReturn(41L, 42L);
        when(pdf.create(any(), any(), any(), any(), any())).thenReturn(new byte[]{1});
        when(files.saveReport(anyInt(), anyInt(), anyLong(), any())).thenReturn(new PlatformFileService.Stored(51L, "report-51.pdf"));
        var service = new ReportWorkflowService(store, access, laboratory, mock(BusinessNotifications.class), pdf, files);
        service.generate(11, "摘要", "结论");
        ArgumentCaptor<List<Map<String, Object>>> items = ArgumentCaptor.forClass(List.class);
        verify(pdf, times(2)).create(any(), any(), any(), any(), items.capture());
        assertEquals(21, items.getAllValues().get(0).get(0).get("orderItemId"));
        assertEquals(1, items.getAllValues().get(0).size());
        assertEquals(22, items.getAllValues().get(1).get(0).get("orderItemId"));
        assertEquals(1, items.getAllValues().get(1).size());
    }
}
