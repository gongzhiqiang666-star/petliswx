package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ClientEventServiceTest {
    final FeatureStore store=mock(FeatureStore.class);
    final AccessService access=mock(AccessService.class);
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final ClientEventService service=new ClientEventService(store,access);
    Map<String,String> event(){ return new HashMap<>(Map.of("id","a1234567-1234-4234-8234-123456789012","event","order_step","step","pet","outcome","view","environment","develop")); }
    void setup(){when(store.jdbc()).thenReturn(jdbc);when(access.current()).thenReturn(new CurrentActor(7,Set.of(PlatformRole.DOCTOR),Set.of(1),Set.of(),new DataScope(false,Set.of(1),Set.of(),null)));}
    @Test void extraPrivateFieldsOrInvalidEnumsRejectWholeBatchBeforeAnyWrite(){
        for(String key:List.of("mobile","orderId","diagnosis","url")){
            var row=event();row.put(key,"private");assertEquals(400,assertThrows(BusinessException.class,()->service.receive(List.of(event(),row))).getCode());
        }
        var row=event();row.put("outcome",null);assertThrows(BusinessException.class,()->service.receive(List.of(row)));
        var bad=event();bad.put("outcome","approved");assertThrows(BusinessException.class,()->service.receive(List.of(bad)));
        verifyNoInteractions(store,access);
    }
    @Test void batchSizeBoundAndUuidValidation(){
        assertThrows(BusinessException.class,()->service.receive(Collections.nCopies(21,event())));
        var row=event();row.put("id","phone-15900000000");assertThrows(BusinessException.class,()->service.receive(List.of(row)));
        verifyNoInteractions(store,access);
    }
    @Test void anonymousInsertDropsIdFromJsonAndNeverStoresActorOrHospital(){
        setup();when(store.encode(any())).thenReturn("{}");service.receive(List.of(event()));
        verify(store).encode(Map.of("event","order_step","step","pet","outcome","view","environment","develop"));
        verify(store).insert(eq("INSERT INTO lin_audit_record(action,resource_type,resource_id,after_json) VALUES('CLIENT_EVENT','MINIPROGRAM_EVENT',?,?)"),eq(event().get("id")),eq("{}"));
        verify(jdbc).queryForList(contains("FOR UPDATE"),eq(event().get("id")));
    }
    @Test void duplicateEventDoesNotInsertAgain(){
        setup();when(jdbc.queryForList(anyString(),anyString())).thenReturn(List.of(Map.of("id",1)));service.receive(List.of(event()));
        verify(store,never()).insert(anyString(),any(Object[].class));
    }
    @Test void summaryRequiresAdminAndBoundedRange(){
        setup();service.summary(7);verify(access).requireAdmin();
        assertThrows(BusinessException.class,()->service.summary(31));
    }
}
