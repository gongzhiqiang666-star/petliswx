package com.petlis.controller;
import com.petlis.controller.api.ReportController;
import com.petlis.config.SecurityConfig;
import com.petlis.dto.response.ReportResponse;
import com.petlis.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
@WebMvcTest(ReportController.class) @Import(SecurityConfig.class)
class ReportControllerTest extends ControllerSecurityTestSupport {
 @Autowired MockMvc mvc;@MockBean ReportService reports;
 @Test void anonymousCannotReadReports()throws Exception{mvc.perform(get("/reports")).andExpect(status().isUnauthorized());verifyNoInteractions(reports);}
 @Test void authenticatedReportListIncludesProtectedPdfUrl()throws Exception{var r=new ReportResponse();r.setReportId(1);r.setPdfFullUrl("https://laboklin.cn/Content/FilePdf/test.pdf");when(reports.queryReports(any())).thenReturn(new PageImpl<>(List.of(r)));mvc.perform(get("/reports").header("Authorization","Bearer test_token_123")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].securePdfUrl").value(org.hamcrest.Matchers.containsString("/api/pdf/view/1")));}
 @Test void paymentRestrictedReportDoesNotIncludePdfUrl()throws Exception{var r=new ReportResponse();r.setReportId(1);r.setPdfFullUrl("https://laboklin.cn/Content/FilePdf/test.pdf");r.setPaymentRestricted(true);when(reports.queryReports(any())).thenReturn(new PageImpl<>(List.of(r)));mvc.perform(get("/reports").header("Authorization","Bearer test_token_123")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].paymentRestricted").value(true)).andExpect(jsonPath("$.data.content[0].securePdfUrl").doesNotExist());}
 @Test void emptyListIsValid()throws Exception{when(reports.queryReports(any())).thenReturn(new PageImpl<>(List.of()));mvc.perform(get("/reports").header("Authorization","Bearer test_token_123")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));}
 @Test void missingReportDownloadReturnsNotFound()throws Exception{when(reports.downloadReportPdf(99)).thenThrow(new IllegalArgumentException("not found"));mvc.perform(get("/reports/99/download").header("Authorization","Bearer test_token_123")).andExpect(status().isBadRequest());}
 @Test void pdfUrlIsAbsoluteAndCarriesReportModalToken()throws Exception{when(reports.getReportPdfUrl(1)).thenReturn(new java.net.URL("https://laboklin.cn/api/pdf/view/1"));mvc.perform(get("/reports/1/pdf-url").header("Authorization","Bearer test_token_123")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value("https://laboklin.cn/api/pdf/view/1?token=test_token_123"));}
}
