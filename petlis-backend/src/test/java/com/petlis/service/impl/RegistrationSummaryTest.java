package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class RegistrationSummaryTest {
    FeatureStore store=mock(FeatureStore.class);
    AccessService access=mock(AccessService.class);
    DashboardService service=new DashboardService(store,access);
    @Test void countsSubmissionCohortAndSeparateCurrentBacklog() {
        var expected=Map.<String,Object>of("submittedCount",3,"pendingCount",1,"approvedCount",1,"rejectedCount",1,"totalPending",7);
        when(store.one(contains("requested_hospital_id IS NOT NULL"),eq(LocalDate.of(2026,11,1)),eq(LocalDate.of(2026,11,8)))).thenReturn(expected);
        assertEquals(expected,service.registrationSummary("2026-11-01","2026-11-08"));
        verify(access).requireAdmin();
        verify(store).one(argThat(sql->sql.contains("p.created_at>=? AND p.created_at<?") && sql.contains("b.review_status='pending'") && !sql.contains("reviewed_at")),eq(LocalDate.of(2026,11,1)),eq(LocalDate.of(2026,11,8)));
    }
    @Test void malformedReversedAndOversizedDateRangesNeverQueryDatabase() {
        for(String[] range:new String[][]{{"bad","2026-11-08"},{"2026-02-30","2026-03-01"},{"2026-11-08","2026-11-08"},{"2026-11-08","2026-11-01"},{"2025-01-01","2026-01-03"}})
            assertThrows(BusinessException.class,()->service.registrationSummary(range[0],range[1]));
        verifyNoInteractions(store);
    }
    @Test void authorizationRunsBeforeAnyQueryOrDateParsing() {
        doThrow(new BusinessException(403,"无权限")).when(access).requireAdmin();
        assertThrows(BusinessException.class,()->service.registrationSummary("bad","bad"));
        verifyNoInteractions(store);
    }
}
