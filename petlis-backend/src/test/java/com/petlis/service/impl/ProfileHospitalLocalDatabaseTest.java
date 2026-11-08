package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class ProfileHospitalLocalDatabaseTest {
 @Autowired FeatureStore store;@Autowired AccountService accounts;@Autowired ReviewService reviews;@Autowired AccessService access;
 @MockBean BusinessNotifications notifications;@MockBean ReviewPermission permission;@MockBean QualificationStorage qualification;@MockBean QualificationOwnershipService ownership;
 int doctor,reviewer,original,target;
 @BeforeEach void fixture(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));String prefix="it_hospital_move_"+UUID.randomUUID().toString().replace("-","").substring(0,10);
  original=(int)store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,CityId,IsActive,PayType) VALUES(?,?,874,1,0)",prefix,prefix);
  target=(int)store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,CityId,IsActive,PayType) VALUES(?,?,874,1,0)",prefix+"b",prefix+"b");doctor=user(prefix,original);reviewer=user(prefix+"r",null);
  store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,title,qualification_files,requested_hospital_id,review_status) VALUES(?,'原职称','[]',?,'approved')",doctor,original);
 }
 int user(String name,Integer hospital){return (int)store.insert("INSERT INTO lin_user(group_id,username,nickname,password,salt,hospital_id,money,score,level,gender,successions,maxsuccessions,loginfailure,status,createtime,updatetime,jointime) VALUES(0,?,'原姓名','','',?,0,0,0,0,0,0,0,'normal',UNIX_TIMESTAMP(),UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",name,hospital);}
 void identity(int user,Integer hospital){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(user,null,hospital,874),null,List.of()));}
 void role(String role,int hospital,boolean active){store.jdbc().update("INSERT INTO lin_user_role(user_id,role_code,hospital_id,scope_type,scope_id,is_active) VALUES(?,?,?,'HOSPITAL',?,?)",doctor,role,hospital,hospital,active);}
 long request(){identity(doctor,original);accounts.requestProfileChange(new AccountService.ProfileChange("新姓名","新职称","/avatar.png",target,List.of("/files/qualifications/"+"a".repeat(32)+".pdf")));return store.jdbc().queryForObject("SELECT MAX(id) FROM lin_profile_change WHERE user_id=?",Long.class,doctor);}
 void approve(long id){identity(reviewer,null);reviews.profileChange(id,new ReviewService.Decision(true,"迁移资料核验通过"));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void approvalMovesDoctorRoleAndRevokesOldHospitalScope(){
  role("DOCTOR",original,true);long id=request();approve(id);identity(doctor,target);access.requireHospital(target);assertEquals(403,assertThrows(BusinessException.class,()->access.requireHospital(original)).getCode());
  assertEquals(target,store.jdbc().queryForObject("SELECT hospital_id FROM lin_user_role WHERE user_id=? AND is_active=1",Integer.class,doctor));
  assertEquals(target,store.jdbc().queryForObject("SELECT scope_id FROM lin_user_role WHERE user_id=? AND is_active=1",Integer.class,doctor));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE action='PROFILE_HOSPITAL_CHANGED' AND resource_id=?",Integer.class,String.valueOf(doctor)));
 }
 @Test void inactiveTargetRoleIsReusedWithoutUniqueKeyFailure(){
  role("DOCTOR",original,true);role("DOCTOR",target,false);approve(request());identity(doctor,target);access.requireHospital(target);assertThrows(BusinessException.class,()->access.requireHospital(original));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_user_role WHERE user_id=? AND is_active=1",Integer.class,doctor));assertEquals(2,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_user_role WHERE user_id=?",Integer.class,doctor));
 }
 @Test void privilegedAccountCannotSilentlyTransferHospitalAdministration(){
  role("DOCTOR",original,true);role("HOSPITAL_ADMIN",original,true);long id=request();assertEquals(409,assertThrows(BusinessException.class,()->approve(id)).getCode());
  assertEquals(original,store.jdbc().queryForObject("SELECT hospital_id FROM lin_user WHERE id=?",Integer.class,doctor));assertEquals("pending",store.jdbc().queryForObject("SELECT status FROM lin_profile_change WHERE id=?",String.class,id));
 }
 @Test void legacyDoctorWithoutRoleRowsUsesNewHospitalAfterApproval(){
  approve(request());identity(doctor,target);access.requireHospital(target);assertThrows(BusinessException.class,()->access.requireHospital(original));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_user_role WHERE user_id=?",Integer.class,doctor));
 }
}
