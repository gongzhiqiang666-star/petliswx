package com.petlis.controller.api;

import com.petlis.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * PDF报告访问Controller
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
@Tag(name = "PDF报告", description = "PDF报告访问相关接口")
public class PdfController {

    private final PdfService pdfService;

    /**
     * 获取PDF报告（带权限验证）
     *
     * @param detailId 诊断详情ID
     * @param request  HTTP请求（用于获取token）
     * @return PDF文件
     */
    @GetMapping("/view/{detailId}")
    @Operation(summary = "获取PDF报告", description = "根据诊断详情ID获取PDF报告文件，需要token验证和权限验证")
    public ResponseEntity<byte[]> getPdfReport(
            @Parameter(description = "诊断详情ID", required = true) @PathVariable Integer detailId,
            HttpServletRequest request) {

        log.info("接收到PDF访问请求, detailId: {}", detailId);

        try {
            // 验证token并获取PDF文件
            byte[] pdfData = pdfService.getPdfWithAuth(detailId, request);

            // 构建响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "report_" + detailId + ".pdf");

            log.info("PDF访问成功, detailId: {}", detailId);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);

        } catch (SecurityException | com.petlis.common.exception.BusinessException e) {
            log.error("PDF访问权限验证失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            log.error("PDF访问参数错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            log.error("PDF文件读取失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取PDF的预览URL（带token参数）
     *
     * @param detailId 诊断详情ID
     * @param request  HTTP请求（用于获取token）
     * @return PDF预览URL
     */
    @GetMapping("/preview-url/{detailId}")
    @Operation(summary = "获取PDF预览URL", description = "获取带token参数的PDF预览URL")
    public ResponseEntity<String> getPdfPreviewUrl(
            @Parameter(description = "诊断详情ID", required = true) @PathVariable Integer detailId,
            HttpServletRequest request) {

        log.info("接收到PDF预览URL请求, detailId: {}", detailId);

        try {
            String previewUrl = pdfService.getPdfPreviewUrlWithAuth(detailId, request);
            log.info("PDF预览URL生成成功, detailId: {}", detailId);
            return ResponseEntity.ok(previewUrl);
        } catch (SecurityException | com.petlis.common.exception.BusinessException e) {
            log.error("PDF预览URL生成失败, 权限验证失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("PDF预览URL生成失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("报告不存在");
        }
    }
}
