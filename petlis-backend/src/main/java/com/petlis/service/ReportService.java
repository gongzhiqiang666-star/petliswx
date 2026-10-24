package com.petlis.service;

import com.petlis.dto.request.ReportQueryRequest;
import com.petlis.dto.response.ReportResponse;
import com.petlis.vo.ReportVO;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.net.URL;

/**
 * 检测报告Service接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
public interface ReportService {

    /**
     * 查询检测报告列表(分页)
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<ReportResponse> queryReports(ReportQueryRequest request);

    /**
     * 获取报告详情
     *
     * @param reportId 报告ID(检测明细ID)
     * @return 报告详情
     */
    ReportVO getReportDetail(Integer reportId);

    /**
     * 下载PDF报告
     *
     * @param reportId 报告ID(检测明细ID)
     * @return PDF文件响应
     */
    ResponseEntity<byte[]> downloadReportPdf(Integer reportId);

    /**
     * 获取PDF报告URL
     *
     * @param reportId 报告ID(检测明细ID)
     * @return PDF文件URL
     */
    URL getReportPdfUrl(Integer reportId);
}
