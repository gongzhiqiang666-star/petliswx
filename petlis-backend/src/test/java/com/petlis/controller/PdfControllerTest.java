package com.petlis.controller;

import com.petlis.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PDF控制器测试类
 *
 * 测试PDF访问API端点
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class PdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdfService pdfService;

    private byte[] testPdfData;

    @BeforeEach
    void setUp() {
        // 准备测试PDF数据
        testPdfData = "Test PDF Content".getBytes();
    }

    /**
     * 测试获取PDF报告（成功）
     */
    @Test
    void testGetPdfReport_Success() throws Exception {
        // Given: 模拟服务返回PDF数据
        when(pdfService.getPdfWithAuth(eq(1), any()))
                .thenReturn(testPdfData);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/pdf/view/1")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(testPdfData))
                .andExpect(header().exists("Content-Disposition"));
    }

    /**
     * 测试获取PDF报告（报告不存在）
     */
    @Test
    void testGetPdfReport_NotFound() throws Exception {
        // Given: 模拟服务抛出异常
        when(pdfService.getPdfWithAuth(eq(999), any()))
                .thenThrow(new IllegalArgumentException("诊断详情不存在"));

        // When & Then: 应该返回400错误
        mockMvc.perform(get("/pdf/view/999")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试获取PDF报告（无权限）
     */
    @Test
    void testGetPdfReport_NoPermission() throws Exception {
        // Given: 模拟服务抛出安全异常
        when(pdfService.getPdfWithAuth(eq(1), any()))
                .thenThrow(new SecurityException("无权访问此报告"));

        // When & Then: 应该返回403错误
        mockMvc.perform(get("/pdf/view/1")
                        .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isForbidden());
    }

    /**
     * 测试获取PDF预览URL（成功）
     */
    @Test
    void testGetPdfPreviewUrl_Success() throws Exception {
        // Given: 模拟服务返回预览URL
        String previewUrl = "https://laboklin.cn/api/pdf/view/1?token=test_token";
        when(pdfService.getPdfPreviewUrlWithAuth(eq(1), any()))
                .thenReturn(previewUrl);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/pdf/preview-url/1")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isOk())
                .andExpect(content().string(previewUrl));
    }

    /**
     * 测试获取PDF预览URL（PDF未生成）
     */
    @Test
    void testGetPdfPreviewUrl_PdfNotGenerated() throws Exception {
        // Given: 模拟服务抛出异常
        when(pdfService.getPdfPreviewUrlWithAuth(eq(1), any()))
                .thenThrow(new IllegalArgumentException("PDF报告尚未生成"));

        // When & Then: 应该返回400错误
        mockMvc.perform(get("/pdf/preview-url/1")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试获取PDF报告（缺少Authorization头）
     */
    @Test
    void testGetPdfReport_MissingAuthHeader() throws Exception {
        // Given: 模拟服务抛出异常（token为空）
        when(pdfService.getPdfWithAuth(eq(1), any()))
                .thenThrow(new IllegalArgumentException("Token不能为空"));

        // When & Then: 应该返回400错误
        mockMvc.perform(get("/pdf/view/1"))
                .andExpect(status().isBadRequest());
    }
}
