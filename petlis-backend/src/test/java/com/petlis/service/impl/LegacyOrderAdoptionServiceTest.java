package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class LegacyOrderAdoptionServiceTest {
    FeatureStore store=mock(FeatureStore.class);
    AccessService access=mock(AccessService.class);
    JdbcTemplate jdbc=mock(JdbcTemplate.class);
    LegacyOrderAdoptionService service=new LegacyOrderAdoptionService(store,access);
    void setup(String amount) {
        when(store.jdbc()).thenReturn(jdbc);
        when(store.one(contains("SELECT o.UserNO"),eq(8))).thenReturn(Map.of("UserNO","BAR-8","InfoHospitalId",4,"SampleStatus",0,"CreateSampleId",7,"TestFee",new BigDecimal(amount),"IsPayMoney",0,"PayType",1));
        when(jdbc.queryForList(contains("lin_order_extension"),eq(8))).thenReturn(List.of());
    }
    @Test void adoptionDoesNotRepriceLegacyAmount() {
        setup("88.00");
        when(jdbc.queryForList(contains("SELECT d.InfoTestTypeId"),eq(8))).thenReturn(List.of(Map.of("InfoTestTypeId",9,"TestPrice",new BigDecimal("88.00"))));
        when(access.current()).thenReturn(new CurrentActor(1,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
        service.adopt(8,2);
        verify(access).requireAdmin();
        verify(jdbc).update(contains("INSERT INTO lin_order_extension"),eq(8),eq(7),eq(2),eq(new BigDecimal("88.00")),eq(new BigDecimal("88.00")),eq("monthly"));
        verify(jdbc,never()).update(contains("UPDATE lin_lb_"),any(Object[].class));
    }
    @Test void inconsistentLegacyAmountIsNotSilentlyCorrected() {
        setup("80.00");
        when(jdbc.queryForList(contains("SELECT d.InfoTestTypeId"),eq(8))).thenReturn(List.of(Map.of("InfoTestTypeId",9,"TestPrice",new BigDecimal("88.00"))));
        assertThrows(BusinessException.class,()->service.adopt(8,2));
        verify(jdbc,never()).update(contains("INSERT"),any(Object[].class));
    }
    @Test void doctorMustBelongToTheOriginalHospital() {
        setup("88.00");
        when(store.one(contains("hospital_id=?"),eq(7),eq(4))).thenThrow(new BusinessException(404,"医生不属于医院"));
        assertThrows(BusinessException.class,()->service.adopt(8,2));
        verify(jdbc,never()).update(contains("INSERT"),any(Object[].class));
    }
    @Test void existingReportsOrShipmentsBlockFreshAdoption() {
        setup("88.00");
        when(jdbc.queryForObject(contains("SELECT EXISTS"),eq(Boolean.class),eq(8),eq(8),eq(8))).thenReturn(true);
        assertThrows(BusinessException.class,()->service.adopt(8,2));
        verify(jdbc,never()).update(contains("INSERT"),any(Object[].class));
    }
    @Test void preparedSubmissionCannotReassignAnExistingDoctor() {
        when(store.jdbc()).thenReturn(jdbc);
        when(store.one(contains("SELECT SampleStatus,CreateSampleId"),eq(8))).thenReturn(Map.of("SampleStatus",0,"CreateSampleId",7,"InfoHospitalId",4));
        when(jdbc.queryForList(contains("SELECT user_id,laboratory_id"),eq(8))).thenReturn(List.of());
        assertThrows(BusinessException.class,()->service.submitPrepared(8,9,2,new BigDecimal("88.00")));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void changedDetailTotalRequiresFreshConfirmationBeforeBinding() {
        when(store.jdbc()).thenReturn(jdbc);
        when(store.one(contains("SELECT SampleStatus,CreateSampleId"),eq(8))).thenReturn(Map.of("SampleStatus",0,"CreateSampleId",0,"InfoHospitalId",4,"TestFee",BigDecimal.ZERO,"IsPayMoney",0));
        when(jdbc.queryForList(contains("SELECT user_id,laboratory_id"),eq(8))).thenReturn(List.of());
        when(store.one(contains("SELECT SUM(TestPrice)"),eq(8))).thenReturn(Map.of("amount",new BigDecimal("90.00")));
        assertThrows(BusinessException.class,()->service.submitPrepared(8,7,2,new BigDecimal("88.00")));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void duplicateBarcodeCannotBeAdopted() {
        setup("88.00");
        when(jdbc.queryForList(contains("WHERE UserNO=?"),eq("BAR-8"),eq(8))).thenReturn(List.of(Map.of("ListDiagnosisProjectId",9)));
        assertThrows(BusinessException.class,()->service.adopt(8,2));
        verify(jdbc,never()).update(contains("INSERT"),any(Object[].class));
    }
    @Test void sameLaboratoryReplayIsReadOnlyButDifferentLaboratoryConflicts() {
        setup("88.00");
        when(jdbc.queryForList(contains("lin_order_extension"),eq(8))).thenReturn(List.of(Map.of("laboratory_id",2)));
        service.adopt(8,2);
        assertThrows(BusinessException.class,()->service.adopt(8,3));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
}
