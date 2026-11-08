package com.petlis.service.impl;

import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Committed synthetic fixture permits proving the service's own transaction rollback. No carrier calls. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class ShipmentReceiptDateLocalDatabaseTest {
    @SpyBean FeatureStore store;
    @MockBean AccessService access;
    @MockBean BusinessNotifications notifications;
    @Autowired ShipmentWorkflowService shipments;
    @Autowired PlatformTransactionManager transactions;
    int order; long shipment; int actor;

    @BeforeEach void fixture() {
        assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));
        var seed=store.one("SELECT order_id,hospital_id,laboratory_id FROM lin_shipment WHERE id=70");
        actor=store.jdbc().queryForObject("SELECT MIN(id) FROM lin_user WHERE status='normal'",Integer.class);
        int hospital=((Number)seed.get("hospital_id")).intValue(),lab=((Number)seed.get("laboratory_id")).intValue();
        when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.LAB_STAFF,PlatformRole.COURIER),Set.of(hospital),Set.of(lab),new DataScope(false,Set.of(hospital),Set.of(lab),actor)));
        new TransactionTemplate(transactions).execute(status->{
            var columns=store.jdbc().queryForList("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION",String.class);
            var names=new ArrayList<String>();var values=new ArrayList<String>();var args=new ArrayList<Object>();
            for(String col:columns){if(col.equals("ListDiagnosisProjectId"))continue;names.add("`"+col+"`");
                if(col.equals("UserNO")){values.add("?");args.add("it_receipt_"+UUID.randomUUID().toString().replace("-","").substring(0,12));}
                else if(col.equals("ReceiveSampleDt"))values.add("'1900-01-01 00:00:00'");
                else if(col.equals("SampleStatus"))values.add("2");else values.add("`"+col+"`");}
            args.add(seed.get("order_id"));
            order=(int)store.insert("INSERT INTO lin_lb_listdiagnosisproject("+String.join(",",names)+") SELECT "+String.join(",",values)+" FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",args.toArray());
            shipment=store.insert("INSERT INTO lin_shipment(order_id,hospital_id,laboratory_id,courier_user_id,tracking_no,status) VALUES(?,?,?,?,?,'DELIVERED')",order,hospital,lab,actor,"IT_RECEIPT_"+UUID.randomUUID().toString().replace("-",""));
            return null;
        });
    }
    @AfterEach void cleanup() {
        reset(store);
        if(order>0)new TransactionTemplate(transactions).execute(status->{
            store.jdbc().update("DELETE FROM lin_lab_task WHERE order_id=?",order);
            store.jdbc().update("DELETE FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id=?",String.valueOf(shipment));
            store.jdbc().update("DELETE FROM lin_shipment WHERE id=? AND order_id=?",shipment,order);
            store.jdbc().update("DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? AND UserNO LIKE 'it_receipt_%'",order);
            return null;
        });
    }
    Object date(){return store.one("SELECT ReceiveSampleDt FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order).get("ReceiveSampleDt");}
    void receive(){shipments.transition(shipment,ShipmentWorkflowService.ShipmentAction.RECEIVE,null,null);}

    @Test void receiptFillsSentinelExactlyFromSignatureAndReplayPreservesIt() {
        receive();Object first=date();
        assertEquals(store.one("SELECT signed_at FROM lin_shipment WHERE id=?",shipment).get("signed_at"),first);
        assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lab_task WHERE shipment_id=?",Integer.class,shipment));
        receive();assertEquals(first,date());
        assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lab_task WHERE shipment_id=?",Integer.class,shipment));
        assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id=? AND action='RECEIVED'",Integer.class,String.valueOf(shipment)));
    }
    @Test void receiptPreservesValidLegacyDate() {
        store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET ReceiveSampleDt='2025-02-28 15:16:17' WHERE ListDiagnosisProjectId=?",order);
        Object previous=date();receive();assertEquals(previous,date());
    }
    @Test void deliveryDoesNotInventLaboratoryReceiptDate() {
        store.jdbc().update("UPDATE lin_shipment SET status='IN_TRANSIT' WHERE id=?",shipment);
        Object previous=date();shipments.transition(shipment,ShipmentWorkflowService.ShipmentAction.DELIVER,null,null);
        assertEquals(previous,date());assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lab_task WHERE shipment_id=?",Integer.class,shipment));
    }
    @Test void concurrentReceiptCreatesOneSignatureDateTaskAndNotification() throws Exception {
        var pool=java.util.concurrent.Executors.newFixedThreadPool(4);
        var start=new java.util.concurrent.CountDownLatch(1);
        try {
            var work=new ArrayList<java.util.concurrent.Future<?>>();
            for(int i=0;i<4;i++)work.add(pool.submit(()->{start.await();receive();return null;}));
            start.countDown();
            for(var result:work)result.get(20,java.util.concurrent.TimeUnit.SECONDS);
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS));}
        assertEquals(store.one("SELECT signed_at FROM lin_shipment WHERE id=?",shipment).get("signed_at"),date());
        assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lab_task WHERE shipment_id=?",Integer.class,shipment));
        assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id=? AND action='RECEIVED'",Integer.class,String.valueOf(shipment)));
        verify(notifications,times(1)).emitForOrder(eq(order),any(),eq("order"),eq("shipment:"+shipment+":RECEIVED"),any(),any(),any());
    }
    @Test void taskFailureRollsBackSignatureDateAndTimeline() {
        Object previous=date();
        doThrow(new IllegalStateException("synthetic receipt task failure")).when(store).insert(startsWith("INSERT INTO lin_lab_task"),any(Object[].class));
        assertThrows(IllegalStateException.class,this::receive);
        var row=store.one("SELECT status,signed_at,version FROM lin_shipment WHERE id=?",shipment);
        assertEquals("DELIVERED",row.get("status"));assertNull(row.get("signed_at"));assertEquals(0,((Number)row.get("version")).intValue());assertEquals(previous,date());
        assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lab_task WHERE shipment_id=?",Integer.class,shipment));
        assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id=?",Integer.class,String.valueOf(shipment)));
        verifyNoInteractions(notifications);
    }
}
