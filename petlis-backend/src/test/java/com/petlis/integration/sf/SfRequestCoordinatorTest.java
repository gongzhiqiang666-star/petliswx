package com.petlis.integration.sf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SfRequestCoordinatorTest {
    static class MemoryStore implements SfRequestCoordinator.Store {
        SfRequestCoordinator.Request row;
        public synchronized SfRequestCoordinator.Request reserve(int order,String op,String key,String external,String hash,int actor) {
            if(row==null) row=new SfRequestCoordinator.Request(1,op,key,external,hash,"RESERVED",null,null);
            return row;
        }
        public synchronized SfRequestCoordinator.Request get(long id) { return row; }
        public synchronized boolean reconcile(long id,String tracking,String result) { finish(id,"SUCCEEDED",null,null,tracking,result);return true; }
        public synchronized boolean reconcileCancellation(long id,String result) {finish(id,"SUCCEEDED","A1000","8037",null,result);return true;}
        public synchronized boolean claim(long id) {
            if(!row.state().equals("RESERVED")) return false;
            finish(id,"SENDING",null,null,null,null);return true;
        }
        public synchronized void finish(long id,String state,String p,String b,String tracking,String result) {
            row=new SfRequestCoordinator.Request(id,row.operation(),row.key(),row.externalOrderNo(),row.payloadHash(),state,tracking,result);
        }
    }
    @Test void timeoutIsDurableAndRepeatDoesNotSendAgain() throws Exception {
        var store=new MemoryStore();var calls=new AtomicInteger();var mapper=new ObjectMapper();
        var gateway=new SfSandboxGateway("fixture","fixture",mapper,form->{calls.incrementAndGet();throw new java.io.IOException("private-carrier-detail");});
        var coordinator=new SfRequestCoordinator(store,gateway,mapper);
        var error=assertThrows(IllegalStateException.class,()->coordinator.dispatch(1,1,"CREATE",Map.of("cargoCount",1)));
        assertFalse(error.getMessage().contains("private-carrier-detail"));
        assertEquals("UNKNOWN",store.row.state());
        assertEquals("UNKNOWN",coordinator.dispatch(1,1,"CREATE",Map.of("cargoCount",1)).state());
        assertEquals(1,calls.get());
        assertThrows(IllegalStateException.class,()->coordinator.dispatch(1,1,"CREATE",Map.of("cargoCount",2)));
        assertEquals(1,calls.get());
    }
    @Test void processCrashInSendingNeverResends() throws Exception {
        var store=new MemoryStore();var mapper=new ObjectMapper();var calls=new AtomicInteger();
        var gateway=new SfSandboxGateway("fixture","fixture",mapper,form->{calls.incrementAndGet();throw new AssertionError("simulated process crash");});
        var coordinator=new SfRequestCoordinator(store,gateway,mapper);
        assertThrows(AssertionError.class,()->coordinator.dispatch(2,1,"CREATE",Map.of()));
        assertEquals("SENDING",coordinator.dispatch(2,1,"CREATE",Map.of()).state());
        assertEquals(1,calls.get());
    }
    @Test void successfulResponsePersistsOnlySafeWaybillFieldsAndIsReplayed() throws Exception {
        var store=new MemoryStore();var mapper=new ObjectMapper();var calls=new AtomicInteger();
        var gateway=new SfSandboxGateway("fixture","fixture",mapper,form->{calls.incrementAndGet();return response("PETLIS_SANDBOX_3");});
        var coordinator=new SfRequestCoordinator(store,gateway,mapper);
        var first=coordinator.dispatch(3,1,"CREATE",Map.of());
        assertEquals("SUCCEEDED",first.state());assertEquals("SF123456",first.trackingNo());
        assertFalse(first.resultJson().contains("private-contact"));
        assertEquals(first,coordinator.dispatch(3,1,"CREATE",Map.of()));assertEquals(1,calls.get());
    }
    @Test void queryRecoversUnknownWithoutCreatingAgain() throws Exception {
        var store=new MemoryStore();var mapper=new ObjectMapper();var calls=new AtomicInteger();
        var gateway=new SfSandboxGateway("fixture","fixture",mapper,form->{
            if(calls.getAndIncrement()==0) throw new java.io.IOException("timeout");
            assertTrue(form.contains("EXP_RECE_SEARCH_ORDER_RESP"));return response("PETLIS_SANDBOX_4");
        });
        var coordinator=new SfRequestCoordinator(store,gateway,mapper);
        assertThrows(IllegalStateException.class,()->coordinator.dispatch(4,1,"CREATE",Map.of()));
        assertEquals("SUCCEEDED",coordinator.reconcileCreate(1).state());
        coordinator.dispatch(4,1,"CREATE",Map.of());assertEquals(2,calls.get());
    }
    @Test void wrongCarrierOrderRemainsUnknown() throws Exception {
        var store=new MemoryStore();var mapper=new ObjectMapper();
        var gateway=new SfSandboxGateway("fixture","fixture",mapper,form->response("wrong-order"));
        var coordinator=new SfRequestCoordinator(store,gateway,mapper);
        assertThrows(IllegalStateException.class,()->coordinator.dispatch(5,1,"CREATE",Map.of()));
        assertEquals("UNKNOWN",store.row.state());
    }
    @Test void cancellationTimeoutNeverRetriesOrBecomesConfirmed() throws Exception {
        var store=new MemoryStore();var calls=new AtomicInteger();var mapper=new ObjectMapper();
        var gateway=new SfSandboxGateway("fixture","fixture",mapper,form->{calls.incrementAndGet();throw new java.io.IOException("timeout");});
        var coordinator=new SfRequestCoordinator(store,gateway,mapper);
        assertThrows(IllegalStateException.class,()->coordinator.dispatch(6,1,"CANCEL",Map.of()));
        assertEquals("UNKNOWN",coordinator.dispatch(6,1,"CANCEL",Map.of()).state());
        assertEquals(1,calls.get());
        assertThrows(IllegalArgumentException.class,()->coordinator.reconcileCreate(1));
    }
    @Test void cancellationLookupConfirmsOnlyExplicitCancelledResultAndDoesNotResend() throws Exception {
        var store=new MemoryStore();var calls=new AtomicInteger();var mapper=new ObjectMapper();
        var gateway=new SfSandboxGateway("fixture","fixture",mapper,form->{
            if(calls.getAndIncrement()==0)throw new java.io.IOException("timeout");
            assertTrue(form.contains("EXP_RECE_SEARCH_ORDER_RESP"));assertFalse(form.contains("EXP_RECE_UPDATE_ORDER"));assertTrue(form.contains("PETLIS_SANDBOX_6"));
            return "{\"apiResultCode\":\"A1000\",\"apiResultData\":{\"success\":false,\"errorCode\":\"8037\"}}";
        });
        var coordinator=new SfRequestCoordinator(store,gateway,mapper);
        assertThrows(IllegalStateException.class,()->coordinator.dispatch(6,1,"CANCEL",Map.of()));
        var result=coordinator.reconcileCancellation(1);assertEquals("SUCCEEDED",result.state());assertTrue(mapper.readTree(result.resultJson()).path("cancelled").asBoolean());
        coordinator.reconcileCancellation(1);assertEquals(2,calls.get());
    }
    @Test void originalWaybillMissingOrderAndPlatformFailureCannotConfirmCancellation() throws Exception {
        for(String response:new String[]{response("PETLIS_SANDBOX_6"),"{\"apiResultCode\":\"A1000\",\"apiResultData\":{\"success\":false,\"errorCode\":\"6150\"}}","{\"apiResultCode\":\"A1009\",\"apiResultData\":{\"success\":false,\"errorCode\":\"8037\"}}"}){
            var store=new MemoryStore();var mapper=new ObjectMapper();var calls=new AtomicInteger();
            var gateway=new SfSandboxGateway("fixture","fixture",mapper,form->{if(calls.getAndIncrement()==0)throw new java.io.IOException("timeout");return response;});
            var coordinator=new SfRequestCoordinator(store,gateway,mapper);assertThrows(IllegalStateException.class,()->coordinator.dispatch(6,1,"CANCEL",Map.of()));
            assertEquals("UNKNOWN",coordinator.reconcileCancellation(1).state());assertEquals(2,calls.get());
        }
    }
    private static String response(String order) {
        return "{\"apiResultCode\":\"A1000\",\"apiResultData\":{\"success\":true,\"errorCode\":\"S0000\",\"msgData\":{\"orderId\":\""+order+"\",\"contact\":\"private-contact\",\"waybillNoInfoList\":[{\"waybillType\":1,\"waybillNo\":\"SF123456\"}]}}}";
    }
}
