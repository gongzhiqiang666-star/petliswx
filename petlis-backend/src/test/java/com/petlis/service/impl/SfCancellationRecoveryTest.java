package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.AccessService;
import com.petlis.integration.sf.SfSandboxConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class SfCancellationRecoveryTest {
    FeatureStore store=mock(FeatureStore.class);AccessService access=mock(AccessService.class);ShipmentWorkflowService shipments=mock(ShipmentWorkflowService.class);JdbcTemplate jdbc=mock(JdbcTemplate.class);SfSandboxConfiguration configuration=mock(SfSandboxConfiguration.class);
    SfShipmentService service=new SfShipmentService(store,access,shipments,new ObjectMapper(),mock(SfRouteEventService.class),configuration);
    void state(String cancellation){when(store.jdbc()).thenReturn(jdbc);when(store.one(contains("SELECT o.InfoHospitalId"),eq(8))).thenReturn(Map.of("InfoHospitalId",4));when(jdbc.queryForList(contains("r.operation='CREATE'"),eq(8))).thenReturn(List.of(Map.of("state","SUCCEEDED","attemptNo",1,"shipmentId",12L,"trackingNo","SF123")));when(jdbc.queryForList(contains("operation='CANCEL'"),eq(8),eq(1))).thenReturn(List.of(Map.of("state",cancellation)));}
    @Test void durableCancellationRepairsLocalShipmentWithoutCarrierCall(){state("SUCCEEDED");service.reconcile(8);verify(access).requireAdmin();verify(shipments).applySfCancellation(12L);verifyNoInteractions(configuration);}
    @Test void unknownCancellationNeverBecomesLocalSuccess(){state("UNKNOWN");service.reconcile(8);verifyNoInteractions(shipments,configuration);}
    @Test void configuredUnknownCancellationLookupFailureLeavesShipmentUnchanged(){
        state("UNKNOWN");when(jdbc.queryForList(contains("operation='CANCEL'"),eq(8),eq(1))).thenReturn(List.of(Map.of("id",33L,"state","UNKNOWN")));
        when(configuration.load()).thenThrow(new com.petlis.common.exception.BusinessException(503,"沙箱暂不可用"));
        assertThrows(com.petlis.common.exception.BusinessException.class,()->service.reconcile(8));verify(configuration).load();verifyNoInteractions(shipments);
    }
}
