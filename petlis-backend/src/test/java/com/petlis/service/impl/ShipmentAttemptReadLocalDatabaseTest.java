package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class ShipmentAttemptReadLocalDatabaseTest {
 @Autowired FeatureStore store;@Autowired AccessService access;@Autowired UserRoleService roles;@Autowired ShipmentWorkflowService shipments;@Autowired SfShipmentService sf;@Autowired SfLabelService labels;@Autowired PlatformFileService files;@Autowired SfRouteEventService routes;
 int admin,hospital,lab,order;long current,oldId,oldLabel;String tracking;
 @BeforeEach void fixture(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));String prefix="it_shipread_"+UUID.randomUUID().toString().replace("-","").substring(0,10);admin=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'寄件历史回归','normal')",prefix);roles.assign(admin,"ADMIN",null,null,admin);identity(admin);
  var old=store.one("SELECT order_id,hospital_id,laboratory_id,status FROM lin_shipment WHERE id=70");order=(int)copy("lin_lb_listdiagnosisproject","ListDiagnosisProjectId",((Number)old.get("order_id")).intValue(),Map.of("UserNO",prefix,"SampleStatus",2));copy("lin_order_extension","order_id",((Number)old.get("order_id")).intValue(),Map.of("order_id",order));hospital=((Number)old.get("hospital_id")).intValue();lab=((Number)old.get("laboratory_id")).intValue();assertEquals("CANCELLED",old.get("status"));
  String oldTracking="IT-OLD-"+UUID.randomUUID().toString().replace("-","");oldId=store.insert("INSERT INTO lin_shipment(order_id,hospital_id,laboratory_id,tracking_no,status,attempt_no,is_current) VALUES(?,?,?,?,'CANCELLED',1,NULL)",order,hospital,lab,oldTracking);
  store.jdbc().update("INSERT INTO lin_sf_request(order_id,attempt_no,operation,request_key,external_order_no,payload_hash,state,tracking_no,created_by) VALUES(?,1,'CREATE',?,?,?,'SUCCEEDED',?,?)",order,UUID.randomUUID().toString(),"IT_OLD_"+order,"a".repeat(64),oldTracking,admin);store.jdbc().update("INSERT INTO lin_sf_request(order_id,attempt_no,operation,request_key,external_order_no,payload_hash,state,created_by) VALUES(?,1,'CANCEL',?,?,?,'SUCCEEDED',?)",order,UUID.randomUUID().toString(),"IT_OLD_"+order,"a".repeat(64),admin);
  oldLabel=files.saveShippingLabel(hospital,lab,oldId,"%PDF-1.4 old test label".getBytes()).id();tracking="IT_READ_"+UUID.randomUUID().toString().replace("-","");current=store.insert("INSERT INTO lin_shipment(order_id,hospital_id,laboratory_id,tracking_no,status,attempt_no,is_current) VALUES(?,?,?,?,'PENDING_PICKUP',2,1)",order,hospital,lab,tracking);
  store.jdbc().update("INSERT INTO lin_sf_request(order_id,attempt_no,operation,request_key,external_order_no,payload_hash,state,tracking_no,created_by) VALUES(?,2,'CREATE',?,?,?,'SUCCEEDED',?,?)",order,UUID.randomUUID().toString(),"IT_LOCAL_NO_CARRIER_"+order,"a".repeat(64),tracking,admin);
 }
 long copy(String table,String pk,int source,Map<String,Object> overrides){var cols=store.jdbc().queryForList("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? ORDER BY ORDINAL_POSITION",String.class,table);var names=new ArrayList<String>();var values=new ArrayList<String>();var args=new ArrayList<Object>();for(String col:cols){if(col.equals(pk)&&!overrides.containsKey(col))continue;names.add("`"+col+"`");if(overrides.containsKey(col)){values.add("?");args.add(overrides.get(col));}else values.add("`"+col+"`");}args.add(source);return store.insert("INSERT INTO `"+table+"`("+String.join(",",names)+") SELECT "+String.join(",",values)+" FROM `"+table+"` WHERE `"+pk+"`=?",args.toArray());}
 void identity(int id){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id,null,hospital,874),null,List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void currentOrderLookupAndRequestStateNeverSelectOldAttempt(){
  assertEquals(current,((Number)shipments.getByOrder(order).get("id")).longValue());var state=sf.status(order);assertEquals(2,((Number)state.get("attemptNo")).intValue());assertEquals(current,((Number)state.get("shipmentId")).longValue());assertFalse(state.containsKey("cancellationState"));
  var history=shipments.history(order);assertEquals(2,history.size());assertEquals(current,((Number)history.get(0).get("id")).longValue());assertEquals(false,history.get(1).get("current"));assertEquals("CANCELLED",shipments.get(oldId).get("status"));
  var page=(List<Map<String,Object>>)shipments.list(null,0,100).get("items");assertTrue(page.stream().noneMatch(row->((Number)row.get("id")).longValue()==oldId));
 }
 @Test void oldCancellationCannotBlockNewLabelAndNewCancellationCannotBeInferredFromOld()throws Exception {
  byte[] pdf="%PDF-1.4 local attempt label".getBytes();var label=files.saveShippingLabel(hospital,lab,current,pdf);assertEquals(label.id(),((Number)labels.get(order).get("id")).longValue());assertArrayEquals(pdf,files.download(label.id()).getBody().getInputStream().readAllBytes());
  assertEquals(409,assertThrows(BusinessException.class,()->files.download(oldLabel)).getCode());assertEquals(409,assertThrows(BusinessException.class,()->shipments.applySfCancellation(current)).getCode());assertEquals("PENDING_PICKUP",shipments.get(current).get("status"));
 }
 @Test void historicalOperationsDeniedAndHistoricalRoutesStayOnTheirShipment()throws Exception {
  assertEquals(409,assertThrows(BusinessException.class,()->shipments.assign(oldId,admin)).getCode());assertEquals(409,assertThrows(BusinessException.class,()->shipments.transition(oldId,ShipmentWorkflowService.ShipmentAction.PICKUP,null,null)).getCode());
  String time=LocalDateTime.now().withNano(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));var row=new ObjectMapper().readTree("{\"mailNo\":\""+store.one("SELECT tracking_no FROM lin_shipment WHERE id=?",oldId).get("tracking_no")+"\",\"routes\":[{\"acceptTime\":\""+time+"\",\"opCode\":\"50\",\"remark\":\"历史测试节点\"}]}");
  String oldTracking=store.one("SELECT tracking_no FROM lin_shipment WHERE id=?",oldId).get("tracking_no").toString();
  // Match the parser's actual gateway batch wrapper; events are retained but cannot mutate the new shipment.
  routes.applyVerifiedBatch(oldId,oldTracking,new ObjectMapper().createObjectNode().set("routeResps",new ObjectMapper().createArrayNode().add(row)));assertEquals("PENDING_PICKUP",shipments.get(current).get("status"));assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_sf_route_event WHERE shipment_id=?",Integer.class,current));
 }
 @Test void previousAttemptCancellationDoesNotBlockCurrentRoute()throws Exception {
  var mapper=new ObjectMapper();var route=mapper.createObjectNode().put("firstStatusCode","1").put("opCode","50").put("acceptTime",LocalDateTime.now().plusSeconds(2).withNano(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).put("remark","本次合成路由");
  var response=mapper.createObjectNode().put("mailNo",tracking);response.set("routes",mapper.createArrayNode().add(route));var data=mapper.createObjectNode();data.set("routeResps",mapper.createArrayNode().add(response));routes.applyVerifiedBatch(current,tracking,data);
  assertEquals("PICKED_UP",shipments.get(current).get("status"));assertEquals("CANCELLED",shipments.get(oldId).get("status"));
 }
 @Test void sameHospitalDoctorCanReadHistoryButForeignDoctorCannot(){
  String prefix="it_read_doc_"+UUID.randomUUID().toString().replace("-","").substring(0,8);int doctor=(int)store.insert("INSERT INTO lin_user(username,nickname,status,hospital_id) VALUES(?,'医生历史回归','normal',?)",prefix,hospital);roles.assign(doctor,"DOCTOR",hospital,null,admin);identity(doctor);assertEquals(2,shipments.history(order).size());roles.remove(store.jdbc().queryForObject("SELECT id FROM lin_user_role WHERE user_id=?",Long.class,doctor),doctor);roles.assign(doctor,"DOCTOR",hospital+100000,null,admin);assertEquals(403,assertThrows(BusinessException.class,()->shipments.history(order)).getCode());
 }
}
