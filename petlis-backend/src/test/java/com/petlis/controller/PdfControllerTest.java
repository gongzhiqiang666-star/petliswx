package com.petlis.controller;
import com.petlis.controller.api.PdfController;
import com.petlis.config.SecurityConfig;
import com.petlis.service.PdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PdfController.class) @Import(SecurityConfig.class)
class PdfControllerTest extends ControllerSecurityTestSupport {
 @Autowired MockMvc mvc;@MockBean PdfService pdf;
 @Test void anonymousPdfAccessIsDenied()throws Exception{mvc.perform(get("/pdf/view/1")).andExpect(status().isUnauthorized());verifyNoInteractions(pdf);}
 @Test void invalidTokenCannotDownloadPdf()throws Exception{mvc.perform(get("/pdf/view/1").header("Authorization","Bearer invalid_token")).andExpect(status().isUnauthorized());verifyNoInteractions(pdf);}
 @Test void authenticatedPdfDownloadReturnsBytes()throws Exception{when(pdf.getPdfWithAuth(eq(1),any())).thenReturn("test-pdf".getBytes());mvc.perform(get("/pdf/view/1").header("Authorization","Bearer test_token_123")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/pdf")).andExpect(content().bytes("test-pdf".getBytes()));}
 @Test void reportModalQueryTokenCanDownloadPdf()throws Exception{when(pdf.getPdfWithAuth(eq(1),any())).thenReturn("test-pdf".getBytes());mvc.perform(get("/pdf/view/1?token=test_token_123")).andExpect(status().isOk()).andExpect(content().bytes("test-pdf".getBytes()));}
 @Test void crossHospitalPdfAccessIsForbidden()throws Exception{when(pdf.getPdfWithAuth(eq(1),any())).thenThrow(new SecurityException("no access"));mvc.perform(get("/pdf/view/1").header("Authorization","Bearer test_token_123")).andExpect(status().isForbidden());}
 @Test void unpaidPdfAccessIsForbidden()throws Exception{when(pdf.getPdfWithAuth(eq(1),any())).thenThrow(new com.petlis.common.exception.BusinessException("订单未支付"));mvc.perform(get("/pdf/view/1").header("Authorization","Bearer test_token_123")).andExpect(status().isForbidden());}
 @Test void missingPdfReturnsNotFound()throws Exception{when(pdf.getPdfWithAuth(eq(1),any())).thenThrow(new IllegalArgumentException("not found"));mvc.perform(get("/pdf/view/1").header("Authorization","Bearer test_token_123")).andExpect(status().isNotFound());}
}
