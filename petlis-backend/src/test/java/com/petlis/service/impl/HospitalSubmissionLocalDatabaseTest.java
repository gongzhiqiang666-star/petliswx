package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class HospitalSubmissionLocalDatabaseTest {
 @Autowired FeatureStore store;@Autowired AccessService access;@Autowired UserRoleService roles;@Autowired HospitalSubmissionService submissions;@Autowired HospitalDocumentService archive;@Autowired PlatformFileService files;
 int admin,manager,doctor,hospital,other;
 @BeforeEach void fixture(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));String prefix="it_doc_review_"+UUID.randomUUID().toString().replace("-","").substring(0,9);hospital=hospital(prefix);other=hospital(prefix+"b");admin=user(prefix+"a");manager=user(prefix+"m");doctor=user(prefix+"d");roles.assign(admin,"ADMIN",null,null,admin);roles.assign(manager,"HOSPITAL_ADMIN",hospital,null,admin);roles.assign(doctor,"DOCTOR",hospital,null,admin);identity(admin);}
 int hospital(String name){return (int)store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,CityId,IsActive,PayType) VALUES(?,?,874,1,0)",name,name);}
 int user(String name){return (int)store.insert("INSERT INTO lin_user(username,nickname,status,hospital_id) VALUES(?,'医院资料回归','normal',?)",name,hospital);}
 void identity(int id){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id,null,hospital,874),null,List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 MockMultipartFile pdf(String text){return new MockMultipartFile("file","执照.pdf","application/pdf",("%PDF-1.4 "+text).getBytes());}
 String key(){return "doc_"+UUID.randomUUID().toString().replace("-","");}
 long id(Map<String,Object> row){return ((Number)row.get("id")).longValue();}
 Map<String,Object> row(long id){return store.one("SELECT biz_type FROM lin_file WHERE id=?",id);}
 @Test void pendingReplacementPreservesOldUntilApprovedAndReplayDoesNotDuplicate()throws Exception {
  long old=id(archive.upload(hospital,pdf("old"),"LICENSE","旧版本",key(),null));identity(manager);String key=key();long next=id(submissions.submit(hospital,pdf("new"),"LICENSE","更新资料",key,old));assertEquals(next,id(submissions.submit(hospital,pdf("new"),"LICENSE","更新资料",key,old)));
  assertEquals("HOSPITAL_DOCUMENT_REVIEW",row(next).get("biz_type"));assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE action='HOSPITAL_DOCUMENT_UPLOADED' AND JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.previousFileId'))=?",Integer.class,String.valueOf(old)));
  assertArrayEquals(pdf("new").getBytes(),files.download(next).getBody().getInputStream().readAllBytes());
  assertEquals(409,assertThrows(BusinessException.class,()->submissions.submit(hospital,pdf("newer"),"LICENSE","重复替换",key(),old)).getCode());identity(admin);assertTrue(((Number)submissions.pending(0,100).get("total")).longValue()>0);String reviewKey=key();var decision=new HospitalSubmissionService.Decision(true,"审核通过",reviewKey);submissions.review(next,decision);submissions.review(next,decision);assertEquals("HOSPITAL_DOCUMENT",row(next).get("biz_type"));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_notification_event WHERE user_id=? AND business_key=?",Integer.class,manager,"hospital-document:"+next));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE action='HOSPITAL_DOCUMENT_REVIEWED' AND resource_id=?",Integer.class,String.valueOf(next)));
  assertEquals(409,assertThrows(BusinessException.class,()->submissions.review(next,new HospitalSubmissionService.Decision(false,"反向审核",reviewKey))).getCode());
  assertEquals(1,((Number)((List<Map<String,Object>>)archive.list(hospital,0,20).get("items")).stream().filter(x->((Number)x.get("id")).longValue()==old).findFirst().orElseThrow().get("replaced")).intValue());
 }
 @Test void rejectionKeepsHistoryAndAllowsCorrectedSubmission(){identity(manager);long first=id(submissions.submit(hospital,pdf("bad"),"LICENSE","初次",key(),null));identity(admin);submissions.review(first,new HospitalSubmissionService.Decision(false,"资料不清晰",key()));identity(manager);var items=(List<Map<String,Object>>)submissions.list(hospital,0,20).get("items");assertEquals("REJECTED",items.get(0).get("reviewStatus"));assertEquals("资料不清晰",items.get(0).get("reviewReason"));assertNotEquals(first,id(submissions.submit(hospital,pdf("correct"),"LICENSE","补充清晰件",key(),null)));}
 @Test void foreignHospitalAndDoctorCannotManageOrDownload()throws Exception {identity(manager);assertEquals(403,assertThrows(BusinessException.class,()->submissions.list(other,0,20)).getCode());long doc=id(submissions.submit(hospital,pdf("own"),"OTHER","本院文件",key(),null));assertEquals(403,assertThrows(BusinessException.class,()->submissions.pending(0,20)).getCode());identity(doctor);assertEquals(403,assertThrows(BusinessException.class,()->submissions.list(hospital,0,20)).getCode());assertEquals(403,assertThrows(BusinessException.class,()->files.download(doc)).getCode());assertEquals(403,assertThrows(BusinessException.class,()->submissions.submit(hospital,pdf("doctor"),"OTHER","医生越权",key(),null)).getCode());}
 @Test void stalePreviousAtApprovalDoesNotOverwriteNewArchive(){long old=id(archive.upload(hospital,pdf("old"),"LICENSE","原件",key(),null));identity(manager);long pending=id(submissions.submit(hospital,pdf("pending"),"LICENSE","待审核",key(),old));identity(admin);archive.upload(hospital,pdf("admin-new"),"LICENSE","后台直接更新",key(),old);assertEquals(409,assertThrows(BusinessException.class,()->submissions.review(pending,new HospitalSubmissionService.Decision(true,"试图覆盖",key()))).getCode());assertEquals("HOSPITAL_DOCUMENT_REVIEW",row(pending).get("biz_type"));}
 @Test void hospitalAdministratorRoleCannotBorrowDoctorScopeOrReviewOwnSubmission(){
  roles.assign(manager,"DOCTOR",other,null,admin);identity(manager);assertEquals(403,assertThrows(BusinessException.class,()->submissions.list(other,0,20)).getCode());var available=submissions.hospitals();assertEquals(1,available.size());assertEquals(hospital,((Number)available.get(0).get("id")).intValue());
  long own=id(submissions.submit(hospital,pdf("self"),"OTHER","本人提交",key(),null));roles.assign(manager,"ADMIN",null,null,admin);assertEquals(403,assertThrows(BusinessException.class,()->submissions.review(own,new HospitalSubmissionService.Decision(true,"本人审核",key()))).getCode());
 }

}
