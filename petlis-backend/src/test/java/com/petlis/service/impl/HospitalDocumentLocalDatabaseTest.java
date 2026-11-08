package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class HospitalDocumentLocalDatabaseTest {
 @Autowired FeatureStore store;
 @TempDir Path root;
 AccessService access;PlatformFileService files;HospitalDocumentService documents;int hospital,other,actor;
 @BeforeEach void setup(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));String prefix="it_document_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
  actor=(int)store.insert("INSERT INTO lin_user(group_id,username,nickname,password,salt,money,score,level,gender,successions,maxsuccessions,loginfailure,status,createtime,updatetime,jointime) VALUES(0,?,'医院资料回归','','',0,0,0,0,0,0,0,'normal',UNIX_TIMESTAMP(),UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",prefix);
  hospital=(int)store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,CityId,IsActive,PayType) VALUES(?,?,874,1,0)",prefix,prefix);
  other=(int)store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,CityId,IsActive,PayType) VALUES(?,?,874,1,0)",prefix+"b",prefix+"b");
  access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  files=new PlatformFileService(store,access,mock(ReviewPermission.class),root.toString(),10485760);documents=new HospitalDocumentService(store,access,files);
 }
 MockMultipartFile pdf(String content){return new MockMultipartFile("file","资料.pdf","application/pdf",("%PDF-1.4 "+content).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 String key(){return "hospital_doc_"+UUID.randomUUID().toString().replace("-","");}
 long id(Map<String,Object> value){return ((Number)value.get("id")).longValue();}
 @Test void uploadReplayReplacementAndOldFileHistoryRemainConsistent() throws Exception {
  String firstKey=key();long first=id(documents.upload(hospital,pdf("first"),"LICENSE","初次归档",firstKey,null));
  assertEquals(first,id(documents.upload(hospital,pdf("first"),"LICENSE","初次归档",firstKey,null)));
  assertEquals(409,assertThrows(BusinessException.class,()->documents.upload(hospital,pdf("changed"),"LICENSE","初次归档",firstKey,null)).getCode());
  long next=id(documents.upload(hospital,pdf("second"),"LICENSE","更新执照",key(),first));assertNotEquals(first,next);
  assertEquals(2L,((Number)documents.list(hospital,0,20).get("total")).longValue());
  assertArrayEquals(pdf("first").getBytes(),files.download(first).getBody().getInputStream().readAllBytes());
  assertTrue(files.download(first).getHeaders().getFirst("Content-Disposition").contains("filename*=UTF-8''"));
  assertEquals("资料.pdf",files.download(first).getHeaders().getContentDisposition().getFilename());
  assertArrayEquals(pdf("second").getBytes(),files.download(next).getBody().getInputStream().readAllBytes());
  assertEquals(409,assertThrows(BusinessException.class,()->documents.upload(hospital,pdf("third"),"LICENSE","过期前序",key(),first)).getCode());
  assertEquals(404,assertThrows(BusinessException.class,()->documents.upload(other,pdf("cross"),"LICENSE","错误医院",key(),next)).getCode());
  try(var stream=Files.list(root)){assertEquals(2,stream.count());}
 }
 @Test void ordinaryDoctorCannotListUploadOrDownloadAndInvalidBytesCreateNoFile() throws Exception {
  var bad=new MockMultipartFile("file","fake.pdf","application/pdf","not pdf".getBytes());assertThrows(BusinessException.class,()->documents.upload(hospital,bad,"LICENSE","无效文件",key(),null));
  try(var stream=Files.list(root)){assertEquals(0,stream.count());}
  long file=id(documents.upload(hospital,pdf("real"),"LICENSE","有效资料",key(),null));
  doThrow(new BusinessException(403,"管理员限定")).when(access).requireAdmin();
  doThrow(new BusinessException(403,"管理员限定")).when(access).requireHospitalAdmin(hospital);
  assertEquals(403,assertThrows(BusinessException.class,()->documents.list(hospital,0,20)).getCode());
  assertEquals(403,assertThrows(BusinessException.class,()->documents.upload(hospital,pdf("real"),"LICENSE","越权",key(),null)).getCode());
  assertEquals(403,assertThrows(BusinessException.class,()->files.download(file)).getCode());
 }
}
