package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class LegacyClinicalServiceTest {
 FeatureStore store=mock(FeatureStore.class);AccessService access=mock(AccessService.class);JdbcTemplate jdbc=mock(JdbcTemplate.class);
 BusinessNotifications notifications=mock(BusinessNotifications.class);LegacyClinicalService service=new LegacyClinicalService(store,access,notifications);
 Map<String,Object> order;
 @BeforeEach void setup(){
  when(store.jdbc()).thenReturn(jdbc);when(access.current()).thenReturn(new CurrentActor(5,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  when(store.encode(any())).thenAnswer(i->new ObjectMapper().writeValueAsString(i.getArgument(0)));
  order=new HashMap<>(Map.of("SampleStatus",0,"IsPayMoney",0,"InfoHospitalId",4,"CreateSampleId",7,"laboratory_id",1,"review_status","pending","AnimalName","QA猫","MasterName","QA畜主","Age","3Y"));
  when(store.one(startsWith("SELECT o.*"),eq(9))).thenAnswer(i->order);
 }
 @SuppressWarnings("unchecked") LegacyClinicalService.Correction command(boolean resubmit){var snapshot=service.read(9);var fields=new HashMap<>((Map<String,String>)snapshot.get("fields"));fields.put("medicalHistory","病史修正");return new LegacyClinicalService.Correction(snapshot.get("version").toString(),fields,"核实原始采样记录",resubmit);}
 @Test void rabiesCorrectionKeepsChipAndSynchronizesDetailRemark(){
  when(jdbc.queryForObject(contains("InfoTestTypeId=1225"),eq(Boolean.class),eq(9))).thenReturn(true);
  var good=command(false);var fields=new HashMap<>(good.fields());
  assertThrows(BusinessException.class,()->service.correct(9,new LegacyClinicalService.Correction(good.version(),fields,good.reason(),false)));
  fields.put("chipNO","QA-CHIP-123");fields.put("remark","QA rabies remark");
  service.correct(9,new LegacyClinicalService.Correction(good.version(),fields,good.reason(),false));
  verify(jdbc).update(contains("SET DetailRemark=?"),eq("QA rabies remark"),eq(9));
 }
 @Test void nonAdminCannotCorrectAndReadUsesHospitalScope(){
  when(access.current()).thenReturn(new CurrentActor(7,Set.of(PlatformRole.DOCTOR),Set.of(99),Set.of(),new DataScope(false,Set.of(99),Set.of(),null)));
  doThrow(new BusinessException(403,"无权访问")).when(access).requireAdmin();
  assertThrows(BusinessException.class,()->service.correct(9,null));
  doThrow(new BusinessException(403,"无权访问")).when(access).requireHospital(4);
  assertThrows(BusinessException.class,()->service.read(9));verify(jdbc,never()).update(anyString(),any(Object[].class));
 }
 @Test void pendingCorrectionKeepsStatusAndDoesNotWritePriceOrIdentity(){
  service.correct(9,command(false));
  verify(jdbc).update(startsWith("UPDATE lin_lb_listdiagnosisproject SET AnimalName=?"),any(Object[].class));
  verify(jdbc,never()).update(contains("SET SampleStatus="),any(Object[].class));
  verify(jdbc,never()).update(contains("TestFee="),any(Object[].class));
  verify(store).insert(contains("'LEGACY_CLINICAL_UPDATED'"),eq(5),eq("9"),anyString(),anyString(),eq("核实原始采样记录"));
  verifyNoInteractions(notifications);
 }
 @Test void rejectedResubmissionResetsReviewAndEmitsPendingOnly(){
  order.put("SampleStatus",1);order.put("review_status","rejected");service.correct(9,command(true));
  verify(jdbc).update(contains("review_status='pending'"),eq(9));verify(jdbc).update(contains("SET SampleStatus=0"),eq(9));verify(notifications).order(9,7,0);
 }
 @Test void staleVersionCannotOverwriteAndCannotResubmitPending(){
  var good=command(false);assertThrows(BusinessException.class,()->service.correct(9,new LegacyClinicalService.Correction("stale",good.fields(),good.reason(),false)));
  assertThrows(BusinessException.class,()->service.correct(9,new LegacyClinicalService.Correction(good.version(),good.fields(),good.reason(),true)));
  verify(jdbc,never()).update(anyString(),any(Object[].class));
 }
 @Test void approvalOrDownstreamLocksClinicalData(){
  order.put("SampleStatus",2);assertEquals(false,service.read(9).get("editable"));
  assertThrows(BusinessException.class,()->service.correct(9,command(false)));
  order.put("SampleStatus",0);when(jdbc.queryForObject(contains("lin_shipment"),eq(Boolean.class),eq(9),eq(9),eq(9),eq(9))).thenReturn(true);
  assertThrows(BusinessException.class,()->service.correct(9,command(false)));verify(jdbc,never()).update(anyString(),any(Object[].class));
 }
 @Test void unexpectedFinancialFieldsOrOverlongClinicalDataAreRejected(){
  var good=command(false);var fields=new HashMap<>(good.fields());fields.put("TestFee","0");assertThrows(BusinessException.class,()->service.correct(9,new LegacyClinicalService.Correction(good.version(),fields,good.reason(),false)));
  fields.remove("TestFee");fields.put("diagnosis","x".repeat(2001));assertThrows(BusinessException.class,()->service.correct(9,new LegacyClinicalService.Correction(good.version(),fields,good.reason(),false)));verify(jdbc,never()).update(anyString(),any(Object[].class));
 }
 @Test void snapshotRevisionPreventsReturningToOldValuesFromRevivingOldVersion(){
  var before=service.read(9).get("version");when(jdbc.queryForList(startsWith("SELECT id FROM lin_audit_record"),eq("9"))).thenReturn(List.of(Map.of("id",123L)));
  assertNotEquals(before,service.read(9).get("version"));
 }
}
