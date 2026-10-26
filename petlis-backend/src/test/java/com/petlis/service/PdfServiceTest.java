package com.petlis.service;

import com.petlis.common.exception.BusinessException;
import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.repository.DiagnosisDetailRepository;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.ListDiagnosisProjectRepository;
import com.petlis.service.impl.PdfServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * PDF服务测试类
 *
 * 测试PDF访问权限验证、文件读取等功能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private DiagnosisDetailRepository detailRepository;

    @Mock
    private ListDiagnosisProjectRepository projectRepository;

    @Mock
    private InfoHospitalRepository hospitalRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PdfServiceImpl pdfService;

    private DiagnosisDetail mockDetail;
    private ListDiagnosisProject mockProject;
    private InfoHospital mockHospital;
    private Path tempPdfFile;

    @BeforeEach
    void setUp() throws Exception {
        // 设置配置值（通过反射注入）
        ReflectionTestUtils.setField(pdfService, "pdfStoragePath", System.getProperty("java.io.tmpdir"));
        ReflectionTestUtils.setField(pdfService, "serverDomain", "https://laboklin.cn");
        ReflectionTestUtils.setField(pdfService, "serverPort", 8082);
        ReflectionTestUtils.setField(pdfService, "contextPath", "/api");

        // 创建临时PDF文件用于测试
        tempPdfFile = Files.createTempFile("test-report", ".pdf");
        Files.write(tempPdfFile, "Test PDF Content".getBytes());

        // 准备模拟数据
        mockDetail = new DiagnosisDetail();
        mockDetail.setDiagnosisDetailId(1);
        mockDetail.setListDiagnosisProjectId(1);
        mockDetail.setIsSendMail(1); // PDF已生成
        mockDetail.setPdfHttpUrl(tempPdfFile.getFileName().toString());

        mockProject = new ListDiagnosisProject();
        mockProject.setListDiagnosisProjectId(1);
        mockProject.setInfoHospitalId(1); // 医院1
        mockProject.setIsPayMoney(1);
        mockHospital = new InfoHospital();
        mockHospital.setInfoHospitalId(1);
        mockHospital.setPayType(0);

        // 模拟请求头
    }

    void allowHospitalAccess() {
        when(hospitalRepository.findById(1)).thenReturn(Optional.of(mockHospital));
    }

    /**
     * 测试成功获取PDF（有权限）
     */
    @Test
    void testGetPdfWithAuth_Success() throws Exception {
        // Given: 模拟查询成功
        when(detailRepository.findById(1)).thenReturn(Optional.of(mockDetail));
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        allowHospitalAccess();

        // 模拟从token中获取医院ID（通过反射调用私有方法需要测试类中暴露）
        // 这里我们模拟SecurityUtils的调用
        try (var mockedStatic = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            mockedStatic.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId)
                    .thenReturn(1); // 当前用户医院ID为1，与项目医院ID一致

            // When: 获取PDF
            byte[] pdfData = pdfService.getPdfWithAuth(1, request);

            // Then: 验证结果
            assertNotNull(pdfData);
            assertEquals("Test PDF Content", new String(pdfData));

            // 验证方法调用
            verify(detailRepository).findById(1);
            verify(projectRepository).findById(1);
        }
    }

    /**
     * 测试获取PDF失败（报告不存在）
     */
    @Test
    void testGetPdfWithAuth_ReportNotFound() {
        // Given: 模拟报告不存在
        when(detailRepository.findById(999)).thenReturn(Optional.empty());

        try (var mockedStatic = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            mockedStatic.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId)
                    .thenReturn(1);

            // When & Then: 应该抛出异常
            assertThrows(IllegalArgumentException.class, () -> {
                pdfService.getPdfWithAuth(999, request);
            });

            verify(detailRepository).findById(999);
            verify(projectRepository, never()).findById(anyInt());
        }
    }

    /**
     * 测试获取PDF失败（PDF未生成）
     */
    @Test
    void testGetPdfWithAuth_PdfNotGenerated() {
        // Given: PDF未生成
        mockDetail.setIsSendMail(0);
        when(detailRepository.findById(1)).thenReturn(Optional.of(mockDetail));

        try (var mockedStatic = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            mockedStatic.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId)
                    .thenReturn(1);

            // When & Then: 应该抛出异常
            assertThrows(IllegalArgumentException.class, () -> {
                pdfService.getPdfWithAuth(1, request);
            });

            verify(detailRepository).findById(1);
        }
    }

    /**
     * 测试获取PDF失败（无权限访问其他医院的报告）
     */
    @Test
    void testGetPdfWithAuth_NoPermission() {
        // Given: 模拟项目属于医院1，但当前用户属于医院2
        when(detailRepository.findById(1)).thenReturn(Optional.of(mockDetail));
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));

        try (var mockedStatic = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            mockedStatic.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId)
                    .thenReturn(2); // 当前用户医院ID为2

            // When & Then: 应该抛出安全异常
            assertThrows(SecurityException.class, () -> {
                pdfService.getPdfWithAuth(1, request);
            });

            verify(detailRepository).findById(1);
            verify(projectRepository).findById(1);
        }
    }

    /**
     * 测试获取PDF预览URL（成功）
     */
    @Test
    void testGetPdfPreviewUrlWithAuth_Success() {
        // Given: 模拟查询成功
        when(detailRepository.findById(1)).thenReturn(Optional.of(mockDetail));
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        allowHospitalAccess();

        try (var mockedStatic = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            mockedStatic.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId)
                    .thenReturn(1);

            // When: 获取预览URL
            String previewUrl = pdfService.getPdfPreviewUrlWithAuth(1, request);

            // Then: 验证URL格式
            assertNotNull(previewUrl);
            assertTrue(previewUrl.startsWith("https://laboklin.cn"));
            assertTrue(previewUrl.contains("/api/pdf/view/1"));
            assertFalse(previewUrl.contains("token="));

            verify(detailRepository).findById(1);
        }
    }

    /**
     * 测试获取PDF预览URL失败（无权限）
     */
    @Test
    void testGetPdfPreviewUrlWithAuth_NoPermission() {
        // Given: 无权限访问
        when(detailRepository.findById(1)).thenReturn(Optional.of(mockDetail));
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));

        try (var mockedStatic = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            mockedStatic.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId)
                    .thenReturn(2);

            // When & Then: 应该抛出异常
            assertThrows(SecurityException.class, () -> {
                pdfService.getPdfPreviewUrlWithAuth(1, request);
            });
        }
    }

    @Test
    void cashUnpaidPdfAndPreviewUrlAreRejected() {
        mockProject.setIsPayMoney(0);
        when(detailRepository.findById(1)).thenReturn(Optional.of(mockDetail));
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        allowHospitalAccess();
        try (var mockedStatic = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            mockedStatic.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId).thenReturn(1);
            assertThrows(BusinessException.class, () -> pdfService.getPdfWithAuth(1, request));
            assertThrows(BusinessException.class, () -> pdfService.getPdfPreviewUrlWithAuth(1, request));
        }
    }

    @Test
    void monthlyUnpaidPdfIsAllowed() throws Exception {
        mockProject.setIsPayMoney(0);
        mockHospital.setPayType(1);
        when(detailRepository.findById(1)).thenReturn(Optional.of(mockDetail));
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        allowHospitalAccess();
        try (var mockedStatic = mockStatic(com.petlis.common.security.SecurityUtils.class)) {
            mockedStatic.when(com.petlis.common.security.SecurityUtils::getCurrentHospitalId).thenReturn(1);
            assertEquals("Test PDF Content", new String(pdfService.getPdfWithAuth(1, request)));
        }
    }

    /**
     * 测试token提取
     */
    @Test
    void testExtractToken_ValidBearerToken() {
        // Given: 有效的Bearer token
        when(request.getHeader("Authorization")).thenReturn("Bearer test_token_123");

        // When: 提取token
        String token = ReflectionTestUtils.invokeMethod(pdfService, "extractToken", request);

        // Then: 验证结果
        assertEquals("test_token_123", token);
    }

    /**
     * 测试token提取（没有Bearer前缀）
     */
    @Test
    void testExtractToken_NoBearerPrefix() {
        // Given: 没有Bearer前缀
        when(request.getHeader("Authorization")).thenReturn("test_token_123");

        // When: 提取token
        String token = ReflectionTestUtils.invokeMethod(pdfService, "extractToken", request);

        // Then: 验证结果（当没有Bearer前缀时，会尝试从参数获取，这里参数为null所以返回null）
        assertNull(token);
    }

    /**
     * 测试token提取（空token）
     */
    @Test
    void testExtractToken_EmptyToken() {
        // Given: 空token
        when(request.getHeader("Authorization")).thenReturn(null);

        // When: 提取token
        String token = ReflectionTestUtils.invokeMethod(pdfService, "extractToken", request);

        // Then: 验证结果（没有Authorization头也没有参数时返回null）
        assertNull(token);
    }
}
