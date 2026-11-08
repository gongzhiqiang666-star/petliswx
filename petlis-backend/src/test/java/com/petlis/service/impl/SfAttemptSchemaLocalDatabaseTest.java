package com.petlis.service.impl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Tests approved local 006 constraints using uncommitted copies of synthetic SF fixtures. */
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class SfAttemptSchemaLocalDatabaseTest {
 @Autowired FeatureStore store;long seed,requestSeed;
 @BeforeEach void guard(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));assertEquals(3,store.jdbc().queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('lin_shipment','lin_sf_request') AND COLUMN_NAME IN ('attempt_no','is_current')",Integer.class));seed=store.jdbc().queryForObject("SELECT s.id FROM lin_shipment s WHERE s.attempt_no=1 AND s.is_current=1 AND NOT EXISTS(SELECT 1 FROM lin_shipment n WHERE n.order_id=s.order_id AND n.attempt_no>1) ORDER BY s.id LIMIT 1",Long.class);requestSeed=store.jdbc().queryForObject("SELECT r.id FROM lin_sf_request r WHERE r.operation='CREATE' AND r.attempt_no=1 AND NOT EXISTS(SELECT 1 FROM lin_sf_request n WHERE n.order_id=r.order_id AND n.attempt_no>1) ORDER BY r.id LIMIT 1",Long.class);}
 String key(){return "it_attempt_"+UUID.randomUUID().toString().replace("-","");}
 long shipment(int attempt,Integer current){return store.insert("INSERT INTO lin_shipment(order_id,hospital_id,laboratory_id,tracking_no,status,attempt_no,is_current) SELECT order_id,hospital_id,laboratory_id,?,'CANCELLED',?,? FROM lin_shipment WHERE id=?",key(),attempt,current,seed);}
 @Test void retainsManyHistoryRowsButOnlyOneCurrentAndOneRowPerAttempt(){
  assertEquals(1,store.jdbc().queryForObject("SELECT attempt_no FROM lin_shipment WHERE id=?",Integer.class,seed));assertEquals(1,store.jdbc().queryForObject("SELECT is_current FROM lin_shipment WHERE id=?",Integer.class,seed));
  long second=shipment(2,null),third=shipment(3,null);assertNotEquals(second,third);
  assertThrows(DataIntegrityViolationException.class,()->shipment(2,null));assertThrows(DataIntegrityViolationException.class,()->shipment(4,1));var invalid=assertThrows(org.springframework.dao.DataAccessException.class,()->shipment(4,0));assertInstanceOf(java.sql.SQLException.class,invalid.getMostSpecificCause());assertEquals(3819,((java.sql.SQLException)invalid.getMostSpecificCause()).getErrorCode());
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_shipment WHERE order_id=(SELECT order_id FROM lin_shipment WHERE id=?) AND is_current=1",Integer.class,seed));
 }
 long request(int attempt,String operation){return store.insert("INSERT INTO lin_sf_request(order_id,operation,request_key,external_order_no,payload_hash,state,created_by,attempt_no) SELECT order_id,?,?,?,payload_hash,'PREPARED',created_by,? FROM lin_sf_request WHERE id=?",operation,key(),key(),attempt,requestSeed);}
 @Test void eachAttemptKeepsIndependentCreateAndCancelRequests(){
  long create=request(2,"CREATE"),cancel=request(2,"CANCEL");assertNotEquals(create,cancel);assertThrows(DataIntegrityViolationException.class,()->request(2,"CREATE"));assertDoesNotThrow(()->request(3,"CREATE"));
 }
}
