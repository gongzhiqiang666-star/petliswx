package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PrintReportLifecycleTest {
    FeatureStore store=mock(FeatureStore.class);AccessService access=mock(AccessService.class);AuditService audits=mock(AuditService.class);
    PrintService service=new PrintService(store,access,audits);
    Map<String,Object> report(){return new HashMap<>(Map.of("hospitalId",1,"laboratoryId",1,"sampleStatus",3,"paid",1,"active",1,"voided",0,"managed",1,"workflowStatus","PUBLISHED","resultStatus","ENTERED","result","中文结论"));}
    void use(Map<String,Object> row){when(store.one(anyString(),eq(81))).thenReturn(row);when(access.current()).thenReturn(new CurrentActor(1,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));}
    @Test void onlyCurrentPublishedActiveResultPrintsAndAudits(){var row=report();row.put("settlementMode","cash");use(row);assertTrue(service.report(81).contains("中文结论"));verify(audits).record(eq("PRINT_REPORT"),any(),eq(81),any(),any(),any());}
    @Test void cancelledUnpublishedVoidedInactiveAndMissingResultsNeverPrintOrAudit(){for(String field:List.of("workflowStatus","resultStatus","active","voided")){var row=report();row.put(field,switch(field){case "active"->0;case "voided"->1;case "workflowStatus"->"CANCELLED";default->"VOIDED";});use(row);assertEquals(409,assertThrows(BusinessException.class,()->service.report(81)).getCode());}var row=report();row.remove("resultStatus");use(row);assertThrows(BusinessException.class,()->service.report(81));verifyNoInteractions(audits);}
    @Test void legacyMonthlyWithoutWorkflowRemainsPrintable(){var row=report();row.put("managed",0);row.remove("workflowStatus");row.remove("resultStatus");row.put("settlementMode","monthly");row.put("paid",0);use(row);assertTrue(service.report(81).contains("中文结论"));verify(store).one(contains("CASE WHEN h.PayType=1"),eq(81));}
    @Test void absentExtensionCannotBypassExistingWorkflowAndRefundedCashIsDenied(){var row=report();row.put("managed",0);row.put("workflowId",9);row.put("workflowStatus","CANCELLED");use(row);assertEquals(409,assertThrows(BusinessException.class,()->service.report(81)).getCode());row=report();row.put("paid",0);row.put("settlementMode","cash");use(row);assertEquals(403,assertThrows(BusinessException.class,()->service.report(81)).getCode());verifyNoInteractions(audits);}
}
