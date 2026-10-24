package com.petlis.controller;

import com.petlis.dto.response.ReportResponse;
import com.petlis.service.ReportService;
import com.petlis.vo.ReportVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 报告控制器测试类
 *
 * 测试报告管理API及安全PDF URL生成
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    private List<ReportResponse> mockReports;
    private ReportVO mockReportDetail;

    @BeforeEach
    void setUp() throws Exception {
        // 准备模拟数据
        mockReports = new ArrayList<>();

        // 已完成且有PDF的报告
        ReportResponse report1 = new ReportResponse();
        report1.setReportId(1);
        report1.setOrderId(1);
        report1.setAnimalName("测试宠物1");
        report1.setTestTypeName("血常规检查");
        report1.setDiagnosisStatus(2); // 已完成
        report1.setTestPrice(new BigDecimal("50.00"));
        report1.setDoctorName("张医生");
        report1.setCollectDt(new Date());
        report1.setCreateDt(new Date());
        report1.setSendMailDt(new Date()); // PDF已生成
        report1.setPdfHttpUrl("/Content/FilePdf/report_1.pdf");
        report1.setPdfFullUrl("https://laboklin.cn/Content/FilePdf/report_1.pdf");
        mockReports.add(report1);

        // 已完成但无PDF的报告
        ReportResponse report2 = new ReportResponse();
        report2.setReportId(2);
        report2.setOrderId(2);
        report2.setAnimalName("测试宠物2");
        report2.setTestTypeName("生化检查");
        report2.setDiagnosisStatus(2);
        report2.setTestPrice(new BigDecimal("80.00"));
        report2.setDoctorName("李医生");
        report2.setCollectDt(new Date());
        report2.setCreateDt(new Date());
        report2.setSendMailDt(null); // PDF未生成
        mockReports.add(report2);

        // 准备报告详情VO
        mockReportDetail = new ReportVO();
        mockReportDetail.setReportId(1);
        mockReportDetail.setAnimalName("测试宠物1");
        mockReportDetail.setTestTypeName("血常规检查");
        mockReportDetail.setDiagnosisStatus(2);
    }

    /**
     * 测试查询报告列表（成功）
     */
    @Test
    void testQueryReports_Success() throws Exception {
        // Given: 模拟服务返回报告列表
        Page<ReportResponse> page = new PageImpl<>(mockReports);
        when(reportService.queryReports(any())).thenReturn(page);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/reports")
                        .header("Authorization", "Bearer test_token_123")
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].reportId").value(1))
                .andExpect(jsonPath("$.data.content[0].animalName").value("测试宠物1"))
                .andExpect(jsonPath("$.data.content[0].securePdfUrl").exists())
                .andExpect(jsonPath("$.data.content[0].securePdfUrl").value(
                        org.hamcrest.Matchers.containsString("/api/pdf/view/1")))
                .andExpect(jsonPath("$.data.content[0].securePdfUrl").value(
                        org.hamcrest.Matchers.containsString("token=test_token_123")))
                .andExpect(jsonPath("$.data.content[1].reportId").value(2))
                .andExpect(jsonPath("$.data.content[1].animalName").value("测试宠物2"))
                .andExpect(jsonPath("$.data.content[1].securePdfUrl").doesNotExist());
    }

    /**
     * 测试查询报告列表（空列表）
     */
    @Test
    void testQueryReports_EmptyList() throws Exception {
        // Given: 模拟服务返回空列表
        Page<ReportResponse> emptyPage = new PageImpl<>(new ArrayList<>());
        when(reportService.queryReports(any())).thenReturn(emptyPage);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/reports")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    /**
     * 测试查询报告列表（无Authorization头）
     */
    @Test
    void testQueryReports_NoAuthHeader() throws Exception {
        // Given: 模拟服务返回报告列表
        Page<ReportResponse> page = new PageImpl<>(mockReports);
        when(reportService.queryReports(any())).thenReturn(page);

        // When & Then: 执行请求（没有Authorization头）
        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // securePdfUrl应该存在但token为空
                .andExpect(jsonPath("$.data.content[0].securePdfUrl").value(
                        org.hamcrest.Matchers.containsString("token=")));
    }

    /**
     * 测试获取报告详情（成功）
     */
    @Test
    void testGetReportDetail_Success() throws Exception {
        // Given: 模拟服务返回报告详情
        when(reportService.getReportDetail(1)).thenReturn(mockReportDetail);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reportId").value(1))
                .andExpect(jsonPath("$.data.animalName").value("测试宠物1"));
    }

    /**
     * 测试获取报告详情（报告不存在）
     */
    @Test
    void testGetReportDetail_NotFound() throws Exception {
        // Given: 模拟服务抛出异常
        when(reportService.getReportDetail(999))
                .thenThrow(new IllegalArgumentException("报告不存在"));

        // When & Then: 应该返回错误
        mockMvc.perform(get("/reports/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 测试下载PDF报告（成功）
     */
    @Test
    void testDownloadReportPdf_Success() throws Exception {
        // Given: 模拟服务返回PDF数据
        byte[] pdfData = "Test PDF Content".getBytes();
        ResponseEntity<byte[]> response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=report_1.pdf")
                .body(pdfData);

        when(reportService.downloadReportPdf(1)).thenReturn(response);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/reports/1/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfData))
                .andExpect(header().exists("Content-Disposition"));
    }

    /**
     * 测试下载PDF报告（失败）
     */
    @Test
    void testDownloadReportPdf_Failure() throws Exception {
        // Given: 模拟服务抛出异常
        when(reportService.downloadReportPdf(999))
                .thenThrow(new RuntimeException("PDF文件不存在"));

        // When & Then: 应该返回400错误
        mockMvc.perform(get("/reports/999/download"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试获取PDF报告URL（成功）
     */
    @Test
    void testGetReportPdfUrl_Success() throws Exception {
        // Given: 模拟服务返回PDF URL
        URL pdfUrl = new URL("https://laboklin.cn/Content/FilePdf/report_1.pdf");
        when(reportService.getReportPdfUrl(1)).thenReturn(pdfUrl);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/reports/1/pdf-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("https://laboklin.cn/Content/FilePdf/report_1.pdf"));
    }

    /**
     * 测试获取PDF报告URL（失败）
     */
    @Test
    void testGetReportPdfUrl_Failure() throws Exception {
        // Given: 模拟服务抛出异常
        when(reportService.getReportPdfUrl(999))
                .thenThrow(new IllegalArgumentException("PDF URL不存在"));

        // When & Then: 应该返回错误
        mockMvc.perform(get("/reports/999/pdf-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 测试查询报告列表（带状态过滤）
     */
    @Test
    void testQueryReports_WithStatusFilter() throws Exception {
        // Given: 准备已完成状态的报告列表
        List<ReportResponse> completedReports = new ArrayList<>();
        ReportResponse completedReport = mockReports.get(0);
        completedReports.add(completedReport);

        Page<ReportResponse> page = new PageImpl<>(completedReports);
        when(reportService.queryReports(any())).thenReturn(page);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/reports")
                        .header("Authorization", "Bearer test_token")
                        .param("status", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].diagnosisStatus").value(2));
    }

    /**
     * 测试查询报告列表（分页）
     */
    @Test
    void testQueryReports_WithPagination() throws Exception {
        // Given: 准备分页数据
        List<ReportResponse> firstPageReports = new ArrayList<>();
        firstPageReports.add(mockReports.get(0));

        Page<ReportResponse> firstPage = new PageImpl<>(firstPageReports);
        when(reportService.queryReports(any())).thenReturn(firstPage);

        // When & Then: 执行请求第一页
        mockMvc.perform(get("/reports")
                        .header("Authorization", "Bearer test_token")
                        .param("pageNum", "0")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    /**
     * 测试安全URL格式验证
     */
    @Test
    void testSecurePdfUrl_Format() throws Exception {
        // Given: 模拟服务返回报告列表
        Page<ReportResponse> page = new PageImpl<>(mockReports);
        when(reportService.queryReports(any())).thenReturn(page);

        // When & Then: 执行请求并验证安全URL格式
        mockMvc.perform(get("/reports")
                        .header("Authorization", "Bearer test_token_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].securePdfUrl").value(
                        org.hamcrest.Matchers.matchesPattern(
                                "https://laboklin.cn/api/pdf/view/1\\?token=test_token_123")))
                .andExpect(jsonPath("$.data.content[0].securePdfUrl").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("pdfFullUrl"))));
    }
}
