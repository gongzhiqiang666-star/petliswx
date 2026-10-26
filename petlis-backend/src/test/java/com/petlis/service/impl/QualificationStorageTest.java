package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class QualificationStorageTest {
 @TempDir Path root;QualificationStorage storage;String reference="/files/qualifications/"+"a".repeat(32)+".pdf";
 @BeforeEach void setup()throws Exception{storage=new QualificationStorage();ReflectionTestUtils.setField(storage,"uploadPath",root.toString());Files.createDirectory(root.resolve("qualifications"));}
 @Test void existingUploadedFileIsAccepted()throws Exception{Files.writeString(root.resolve("qualifications/"+"a".repeat(32)+".pdf"),"%PDF-1.7");assertDoesNotThrow(()->storage.validate(List.of(reference)));}
 @Test void nonexistentFileCannotBeUsedAsProof(){assertThrows(BusinessException.class,()->storage.validate(List.of(reference)));}
 @Test void pathTraversalIsRejected(){assertThrows(BusinessException.class,()->storage.validate(List.of("/files/qualifications/../../secret.pdf")));}
 @Test void repeatedReferencesAreRejected(){assertThrows(BusinessException.class,()->storage.validate(List.of(reference,reference)));}
 @Test void symbolicLinkCannotEscapeUploadFolder()throws Exception{Path outside=root.resolve("outside.pdf");Files.writeString(outside,"%PDF-1.7");Files.createSymbolicLink(root.resolve("qualifications/"+"a".repeat(32)+".pdf"),outside);assertThrows(BusinessException.class,()->storage.validate(List.of(reference)));}
}
