package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.dto.request.ReportQueryRequest;
import com.petlis.dto.response.ReportResponse;
import com.petlis.service.ReportService;
import com.petlis.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URL;

/**
 * 检测报告管理Controller
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("reports")
@RequiredArgsConstructor
@Tag(name = "检测报告管理", description = "检测报告的查询、详情查看、下载等操作")
public class ReportController {

    private final ReportService reportService;

    /**
     * 服务器域名（用于生成安全PDF URL）
     */
    @Value("${server.domain}")
    private String serverDomain;

    /**
     * 查询检测报告列表
     *
     * @param request 查询请求
     * @param httpRequest HTTP请求（用于获取token）
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询检测报告列表", description = "根据条件查询检测报告列表,支持按宠物、状态、日期筛选")
    public Result<Page<ReportResponse>> queryReports(
            ReportQueryRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("接收到查询检测报告列表请求, 参数: {}", request);
        try {
            Page<ReportResponse> result = reportService.queryReports(request);

            // 填充安全PDF URL（带token验证）
            fillSecurePdfUrls(result, httpRequest);

            return Result.success(result);
        } catch (Exception e) {
            log.error("查询检测报告列表失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取检测报告详情
     *
     * @param id 报告ID (检测明细ID)
     * @return 报告详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取检测报告详情", description = "获取指定报告的详细信息,包含完整的报告内容")
    public Result<ReportVO> getReportDetail(
            @Parameter(description = "报告ID", required = true) @PathVariable Integer id
    ) {
        log.info("接收到获取检测报告详情请求, 报告ID: {}", id);
        try {
            ReportVO vo = reportService.getReportDetail(id);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取检测报告详情失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 下载PDF报告
     *
     * @param id 报告ID (检测明细ID)
     * @return PDF文件
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "下载PDF报告", description = "下载指定报告的PDF文件")
    public ResponseEntity<byte[]> downloadReportPdf(
            @Parameter(description = "报告ID", required = true) @PathVariable Integer id
    ) {
        log.info("接收到下载PDF报告请求, 报告ID: {}", id);
        try {
            return reportService.downloadReportPdf(id);
        } catch (Exception e) {
            log.error("下载PDF报告失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取PDF报告URL
     *
     * @param id 报告ID (检测明细ID)
     * @return PDF文件URL
     */
    @GetMapping("/{id}/pdf-url")
    @Operation(summary = "获取PDF报告URL", description = "获取指定报告的PDF文件下载链接")
    public Result<String> getReportPdfUrl(
            @Parameter(description = "报告ID", required = true) @PathVariable Integer id
    ) {
        log.info("接收到获取PDF报告URL请求, 报告ID: {}", id);
        try {
            URL url = reportService.getReportPdfUrl(id);
            return Result.success(url.toString());
        } catch (Exception e) {
            log.error("获取PDF报告URL失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 填充安全PDF URL（带token验证）
     */
    private void fillSecurePdfUrls(Page<ReportResponse> result, HttpServletRequest request) {
        if (result.getContent() == null || result.getContent().isEmpty()) {
            return;
        }

        // 获取token
        String token = extractToken(request);

        // 为每个报告填充安全PDF URL
        result.getContent().forEach(report -> {
            if (!Boolean.TRUE.equals(report.getPaymentRestricted())
                    && report.getPdfFullUrl() != null && !report.getPdfFullUrl().isEmpty()) {
                // 生成安全访问URL：/api/pdf/view/{reportId}?token={token}
                String secureUrl = String.format("%s/api/pdf/view/%d",
                        serverDomain,
                        report.getReportId());
                report.setSecurePdfUrl(secureUrl);
            }
        });
    }

    /**
     * 从请求中提取token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return "";
    }
}
