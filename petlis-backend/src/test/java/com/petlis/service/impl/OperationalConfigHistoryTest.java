package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class OperationalConfigHistoryTest {
    FeatureStore store=mock(FeatureStore.class);JdbcTemplate jdbc=mock(JdbcTemplate.class);ReviewPermission permission=mock(ReviewPermission.class);
    OperationalService service=new OperationalService(store,permission);
    @BeforeEach void setup() {
        when(store.jdbc()).thenReturn(jdbc);
        when(store.encode(any())).thenAnswer(call->new ObjectMapper().writeValueAsString(call.getArgument(0)));
        when(store.one(contains("lin_operational_config"),eq("qa.config"))).thenReturn(Map.of("version",3,"config_value","{\"enabled\":false}"));
        when(jdbc.update(contains("UPDATE lin_operational_config"),any(Object[].class))).thenReturn(1);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(5,null,7,1),null,List.of()));
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void restoreCreatesNewVersionAndAuditsBothSnapshotsAndSource() {
        when(store.one(contains("lin_audit_record"),eq(19L),eq("qa.config"))).thenReturn(Map.of("after_json","{\"scopeType\":\"GLOBAL\",\"scopeId\":0,\"version\":1,\"value\":{\"enabled\":true}}"));
        service.restoreConfig("qa.config",19,3,"恢复验证");
        verify(jdbc).update(contains("UPDATE lin_operational_config"),eq("{\"enabled\":true}"),eq(5),eq("qa.config"),eq(3));
        verify(jdbc).update(contains("before_json,after_json"),eq(5),eq("OPERATIONAL_CONFIG_RESTORED"),eq("OPERATIONAL_CONFIG"),eq("qa.config"),contains("\"version\":3"),argThat((String json)->json.contains("\"version\":4") && json.contains("\"restoredFromAuditId\":19")),eq("恢复验证"));
    }
    @Test void beforeSnapshotOfFirstRecordedEditCanRestoreLegacyValue() {
        when(store.one(contains("lin_audit_record"),eq(19L),eq("qa.config"))).thenReturn(Map.of("before_json","{\"scopeType\":\"GLOBAL\",\"scopeId\":0,\"version\":2,\"value\":{\"legacy\":true}}","after_json","{}"));
        service.restoreConfig("qa.config",19,3,"恢复首次记录前值","BEFORE");
        verify(jdbc).update(contains("UPDATE lin_operational_config"),eq("{\"legacy\":true}"),eq(5),eq("qa.config"),eq(3));
        assertThrows(BusinessException.class,()->service.restoreConfig("qa.config",19,3,"原因","OTHER"));
    }
    @Test void staleVersionAndMissingReasonNeverWrite() {
        assertThrows(BusinessException.class,()->service.restoreConfig("qa.config",19,2,"原因"));
        assertThrows(BusinessException.class,()->service.restoreConfig("qa.config",19,3," "));
        verifyNoInteractions(jdbc);
    }
    @Test void legacyMissingMalformedAndWrongScopeSnapshotsCannotRestore() {
        for(String json:List.of("null","{}","[]","{\"scopeType\":\"HOSPITAL\",\"scopeId\":7,\"version\":1,\"value\":{\"enabled\":true}}")) {
            when(store.one(contains("lin_audit_record"),eq(19L),eq("qa.config"))).thenReturn(Map.of("after_json",json));
            assertThrows(BusinessException.class,()->service.restoreConfig("qa.config",19,3,"原因"));
        }
        verifyNoInteractions(jdbc);
    }
    @Test void unauthorizedRestoreAndHistoryNeverReadSnapshots() {
        doThrow(new BusinessException(403,"无权限")).when(permission).requireReviewer();
        assertThrows(BusinessException.class,()->service.restoreConfig("qa.config",19,3,"原因"));
        assertThrows(BusinessException.class,()->service.configHistory("qa.config",0,20));
        verifyNoInteractions(store);
    }
}
