package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class InvoiceFileServiceTest {
 FeatureStore store=mock(FeatureStore.class);ReviewPermission permission=mock(ReviewPermission.class);PlatformFileService files=mock(PlatformFileService.class);BusinessNotifications notifications=mock(BusinessNotifications.class);JdbcTemplate jdbc=mock(JdbcTemplate.class);
 InvoiceFileService service=new InvoiceFileService(store,permission,files,notifications);
 @BeforeEach void login(){when(store.jdbc()).thenReturn(jdbc);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(7,null,4,1),null,List.of()));}
 @AfterEach void logout(){SecurityContextHolder.clearContext();}
 @Test void unrelatedDoctorCannotDownloadEvenBeforeFileLookup(){when(store.one(contains("SELECT status,user_id"),eq(3L))).thenReturn(Map.of("status","issued","user_id",8));assertThrows(BusinessException.class,()->service.download(3));verifyNoInteractions(files);}
 @Test void pendingInvoiceCannotDownload(){when(store.one(contains("SELECT status,user_id"),eq(3L))).thenReturn(Map.of("status","pending","user_id",7));assertThrows(BusinessException.class,()->service.download(3));verifyNoInteractions(files);}
 @Test void repeatedIssueCannotStoreAnotherPdfOrNotify(){when(store.one(contains("JOIN lin_user"),eq(3L))).thenReturn(Map.of("status","issued","user_id",8,"hospital_id",4));assertThrows(BusinessException.class,()->service.issue(3,new MockMultipartFile("file","invoice.pdf","application/pdf",new byte[]{37,80,68,70,45})));verify(permission).requireReviewer();verifyNoInteractions(files,notifications);}
}
