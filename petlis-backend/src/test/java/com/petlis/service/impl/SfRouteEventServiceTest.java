package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class SfRouteEventServiceTest {
    private final ObjectMapper mapper=new ObjectMapper();
    @Test void carrierDeliveryNeverBecomesLaboratoryReceipt() throws Exception {
        String target=SfRouteEventService.mappedStatus(mapper.readTree("{\"firstStatusCode\":\"4\",\"opCode\":\"80\"}"));
        assertEquals("DELIVERED",target);
        var now=LocalDateTime.now();
        assertFalse(SfRouteEventService.canAdvance("RECEIVED",target,now,null));
        assertTrue(SfRouteEventService.canAdvance("PENDING_PICKUP",target,now,null));
    }
    @Test void olderAndLowerRankNodesCannotRollBackState() {
        var now=LocalDateTime.now();
        assertFalse(SfRouteEventService.canAdvance("PICKED_UP","IN_TRANSIT",now.minusHours(1),now));
        assertFalse(SfRouteEventService.canAdvance("DELIVERED","IN_TRANSIT",now,null));
        assertFalse(SfRouteEventService.canAdvance("IN_TRANSIT","IN_TRANSIT",now,null));
    }
    @Test void unrecognizedPrimaryCodeIsTimelineOnlyEvenWithRecognizedLegacyOpcode() throws Exception {
        assertNull(SfRouteEventService.mappedStatus(mapper.readTree("{\"firstStatusCode\":\"999\",\"opCode\":\"80\"}")));
    }
    @Test void eventDedupUsesBusinessFieldsAndUnambiguousEncoding() {
        var now=LocalDateTime.now();
        assertEquals(SfRouteEventService.eventKey("SF1","80",now,"签收","市"),SfRouteEventService.eventKey("SF1","80",now,"签收","市"));
        assertNotEquals(SfRouteEventService.eventKey("SF1","80",now,"ab","c"),SfRouteEventService.eventKey("SF1","80",now,"a","bc"));
    }

    @Test void entireAuthenticatedBatchIsValidatedBeforeDatabaseAccess() throws Exception {
        var store=org.mockito.Mockito.mock(FeatureStore.class);
        var notifications=org.mockito.Mockito.mock(BusinessNotifications.class);
        var service=new SfRouteEventService(store,notifications);
        String good="{\"opCode\":\"50\",\"acceptTime\":\"2026-11-07 12:00:00\"}";
        String bad="{\"opCode\":\"80\",\"acceptTime\":\"2026-02-30 12:00:00\"}";
        var invalid=mapper.readTree("{\"routeResps\":[{\"mailNo\":\"SF123456\",\"routes\":["+good+","+bad+"]}]}");
        assertThrows(com.petlis.common.exception.BusinessException.class,()->service.applyVerifiedBatch(1,"SF123456",invalid));
        var mismatched=mapper.readTree("{\"routeResps\":[{\"mailNo\":\"SF123456\",\"routes\":["+good+"]},{\"mailNo\":\"SF999999\",\"routes\":[]}]}");
        assertThrows(com.petlis.common.exception.BusinessException.class,()->service.applyVerifiedBatch(1,"SF123456",mismatched));
        for(String shape:java.util.List.of("{}","{\"routeResps\":{}}","{\"routeResps\":[{\"mailNo\":\"SF123456\"}]}")) {
            var data=mapper.readTree(shape);
            assertThrows(com.petlis.common.exception.BusinessException.class,()->service.applyVerifiedBatch(1,"SF123456",data));
        }
        org.mockito.Mockito.verifyNoInteractions(store,notifications);
    }
    @Test void shipmentLinkMustStillMatchWhenTransactionAcquiresLock() throws Exception {
        var store=org.mockito.Mockito.mock(FeatureStore.class);
        var notifications=org.mockito.Mockito.mock(BusinessNotifications.class);
        org.mockito.Mockito.when(store.one(org.mockito.ArgumentMatchers.contains("FOR UPDATE"),org.mockito.ArgumentMatchers.eq(1L)))
            .thenReturn(java.util.Map.of("tracking_no","SF999999","external_order_no","PETLIS-1"));
        var service=new SfRouteEventService(store,notifications);
        var data=mapper.readTree("{\"routeResps\":[{\"mailNo\":\"SF123456\",\"routes\":[{\"opCode\":\"50\",\"acceptTime\":\"2026-11-07 12:00:00\"}]}]}");
        assertThrows(com.petlis.common.exception.BusinessException.class,()->service.applyVerifiedBatch(1,"SF123456",data));
        org.mockito.Mockito.verify(store,org.mockito.Mockito.never()).jdbc();
        org.mockito.Mockito.verifyNoInteractions(notifications);
    }
}
