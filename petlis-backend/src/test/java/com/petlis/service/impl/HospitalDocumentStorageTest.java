package com.petlis.service.impl;
import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class HospitalDocumentStorageTest {
 @TempDir Path root;
 @Test void failedMetadataWriteDoesNotLeaveUntrackedUpload() throws Exception {
  var store=mock(FeatureStore.class);var access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(7,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  when(store.insert(anyString(),any(Object[].class))).thenThrow(new IllegalStateException("simulated metadata failure"));
  var service=new PlatformFileService(store,access,mock(ReviewPermission.class),root.toString(),10000);
  assertThrows(IllegalStateException.class,()->service.saveHospitalDocument(1,new MockMultipartFile("file","test.pdf","application/pdf","%PDF-1.4 test".getBytes())));
  try(var files=Files.list(root)){assertEquals(0,files.count());}
 }
 @Test void laterTransactionRollbackRemovesOnlyItsOwnNewFile() throws Exception {
  var store=mock(FeatureStore.class);var access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(7,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));when(store.insert(anyString(),any(Object[].class))).thenReturn(51L);
  Files.writeString(root.resolve("existing.pdf"),"keep existing");var service=new PlatformFileService(store,access,mock(ReviewPermission.class),root.toString(),10000);
  org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
  try{
   service.saveHospitalDocument(1,new MockMultipartFile("file","test.pdf","application/pdf","%PDF-1.4 test".getBytes()));
   for(var callback:org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations())callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
   try(var files=Files.list(root)){assertEquals(1,files.count());}assertEquals("keep existing",Files.readString(root.resolve("existing.pdf")));
  }finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
 }
}
