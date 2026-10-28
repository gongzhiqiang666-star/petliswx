package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.repository.DiagnosisDetailRepository;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.ListDiagnosisProjectRepository;
import com.petlis.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PDF报告服务实现
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final DiagnosisDetailRepository detailRepository;
    private final ListDiagnosisProjectRepository projectRepository;
    private final InfoHospitalRepository hospitalRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * PDF文件存储路径（从配置文件读取）
     */
    @Value("${app.pdf.storage-path}")
    private String pdfStoragePath;

    /**
     * 服务器域名（用于生成预览URL）
     */
    @Value("${server.domain}")
    private String serverDomain;

    /**
     * 服务器端口
     */
    @Value("${server.port}")
    private Integer serverPort;

    /**
     * Context path
     */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    public byte[] getPdfWithAuth(Integer detailId, HttpServletRequest request)
            throws SecurityException, IllegalArgumentException, IOException {

        log.info("开始获取PDF文件, detailId: {}", detailId);

        // 1. 从token中获取当前用户的医院ID
        Integer currentHospitalId = getCurrentHospitalId(request);
        log.info("当前用户医院ID: {}", currentHospitalId);

        // 2. 查询诊断详情
        DiagnosisDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("诊断详情不存在, detailId: " + detailId));

        // 3. 验证PDF是否已生成
        if (detail.getIsSendMail() == null || detail.getIsSendMail() != 1) {
            throw new IllegalArgumentException("PDF报告尚未生成");
        }

        String pdfHttpUrl = detail.getPdfHttpUrl();
        if (pdfHttpUrl == null || pdfHttpUrl.isEmpty() || pdfHttpUrl.equals("/Content/FilePdf/")) {
            throw new IllegalArgumentException("PDF报告尚未生成");
        }

        // 4. 验证权限：检查用户是否有权访问该PDF
        ListDiagnosisProject project = projectRepository.findById(detail.getListDiagnosisProjectId())
                .orElseThrow(() -> new IllegalArgumentException("关联的订单不存在"));

        if (!project.getInfoHospitalId().equals(currentHospitalId)) {
            log.warn("权限验证失败: 用户医院ID({}) 与 PDF所属医院ID({}) 不匹配",
                    currentHospitalId, project.getInfoHospitalId());
            throw new SecurityException("无权访问此报告");
        }
        assertPaymentAllowed(project);

        log.info("权限验证通过, 开始读取PDF文件: {}", pdfHttpUrl);

        // 5. 读取PDF文件
        String pdfFilePath = buildPdfFilePath(pdfHttpUrl);
        Path path = Paths.get(pdfFilePath);

        if (!Files.exists(path)) {
            log.error("PDF文件不存在: {}", pdfFilePath);
            throw new IllegalArgumentException("PDF文件不存在");
        }

        byte[] pdfData = Files.readAllBytes(path);
        log.info("PDF文件读取成功, 大小: {} bytes", pdfData.length);

        return pdfData;
    }

    @Override
    public String getPdfPreviewUrlWithAuth(Integer detailId, HttpServletRequest request)
            throws SecurityException, IllegalArgumentException {

        log.info("开始生成PDF预览URL, detailId: {}", detailId);

        // 1. 验证权限
        Integer currentHospitalId = getCurrentHospitalId(request);

        DiagnosisDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("诊断详情不存在"));

        // 验证PDF是否已生成
        if (detail.getIsSendMail() == null || detail.getIsSendMail() != 1) {
            throw new IllegalArgumentException("PDF报告尚未生成");
        }

        String pdfHttpUrl = detail.getPdfHttpUrl();
        if (pdfHttpUrl == null || pdfHttpUrl.isEmpty() || pdfHttpUrl.equals("/Content/FilePdf/")) {
            throw new IllegalArgumentException("PDF报告尚未生成");
        }

        // 验证权限
        ListDiagnosisProject project = projectRepository.findById(detail.getListDiagnosisProjectId())
                .orElseThrow(() -> new IllegalArgumentException("关联的订单不存在"));

        if (!project.getInfoHospitalId().equals(currentHospitalId)) {
            throw new SecurityException("无权访问此报告");
        }
        assertPaymentAllowed(project);

        String previewUrl = String.format("%s%s/pdf/view/%d",serverDomain,contextPath,detailId);
        return previewUrl;
    }

    /**
     * 从token中获取当前用户的医院ID
     */
    private Integer getCurrentHospitalId(HttpServletRequest request) {
        // 从SecurityContextHolder获取当前用户的医院ID
        return SecurityUtils.getCurrentHospitalId();
    }

    private void assertPaymentAllowed(ListDiagnosisProject project) {
        InfoHospital hospital = hospitalRepository.findById(project.getInfoHospitalId())
                .orElseThrow(() -> new BusinessException("订单所属医院不存在"));
        String settlementMode;
        try {
            settlementMode = jdbcTemplate.queryForObject("SELECT settlement_mode FROM lin_order_extension WHERE order_id=?", String.class, project.getListDiagnosisProjectId());
        } catch (org.springframework.dao.EmptyResultDataAccessException ignored) {
            settlementMode = null;
        }
        boolean monthly = settlementMode == null ? Integer.valueOf(1).equals(hospital.getPayType()) : "monthly".equalsIgnoreCase(settlementMode);
        if (!monthly && !Integer.valueOf(1).equals(project.getIsPayMoney())) {
            throw new BusinessException("订单未支付，报告暂未开放，请完成支付后查看");
        }
    }

    /**
     * 从请求中提取token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 也尝试从查询参数中获取token（用于web-view场景）
        String paramToken = request.getParameter("token");
        if (paramToken != null && !paramToken.isEmpty()) {
            return paramToken;
        }

        return null;
    }

    /**
     * 构建PDF文件的物理路径
     */
    private String buildPdfFilePath(String pdfHttpUrl) {
        Path root=Paths.get(pdfStoragePath).toAbsolutePath().normalize();
        String relative=pdfHttpUrl.replace('\\','/');
        if(relative.startsWith("http://") || relative.startsWith("https://")) {
            try { relative = java.net.URI.create(relative).getPath(); }
            catch (IllegalArgumentException error) { throw new SecurityException("报告路径无效"); }
        }
        var apiPdf = relative.matches(".*/api/pdf/view/\\d+") ? relative : relative.matches("/api/pdf/view/\\d+") ? relative : null;
        if (apiPdf != null) {
            int detailId = Integer.parseInt(apiPdf.substring(apiPdf.lastIndexOf('/') + 1));
            relative = jdbcTemplate.queryForObject("SELECT f.storage_path FROM lin_file f JOIN lin_report_workflow w ON w.pdf_file_id=f.id WHERE w.detail_id=?", String.class, detailId);
            if (relative == null) throw new IllegalArgumentException("报告文件不可用");
        }
        if(relative.startsWith("/Content/FilePdf/"))relative=relative.substring("/Content/FilePdf/".length());
        else if(relative.startsWith("Content/FilePdf/"))relative=relative.substring("Content/FilePdf/".length());
        if(relative.startsWith("/") || relative.contains(":") || !relative.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf"))throw new SecurityException("报告路径无效");
        Path candidate=root.resolve(relative).normalize();
        if(!candidate.startsWith(root))throw new SecurityException("报告路径超出允许目录");
        try{if(Files.exists(candidate) && !candidate.toRealPath().startsWith(root.toRealPath()))throw new SecurityException("报告路径超出允许目录");}catch(IOException e){throw new IllegalArgumentException("报告文件不可用");}
        return candidate.toString();
    }

    /**
     * 构建PDF预览URL（带token参数）
     */
    private String buildPdfPreviewUrl(Integer detailId, String token) {
        // 格式: https://domain/api/pdf/view/{detailId}?token={token}
        return String.format("%s/api/pdf/view/%d?token=%s",
                serverDomain,
                detailId,
                token);
    }
}
