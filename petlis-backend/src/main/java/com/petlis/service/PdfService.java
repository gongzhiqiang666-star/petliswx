package com.petlis.service;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * PDF报告服务接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
public interface PdfService {

    /**
     * 获取PDF文件（带权限验证）
     *
     * @param detailId 诊断详情ID
     * @param request  HTTP请求（用于获取token）
     * @return PDF文件字节数组
     * @throws SecurityException 权限验证失败
     * @throws IllegalArgumentException PDF不存在
     * @throws java.io.IOException 文件读取失败
     */
    byte[] getPdfWithAuth(Integer detailId, HttpServletRequest request)
            throws SecurityException, IllegalArgumentException, IOException;

    /**
     * 获取PDF预览URL（带token参数）
     *
     * @param detailId 诊断详情ID
     * @param request  HTTP请求（用于获取token）
     * @return PDF预览URL
     * @throws SecurityException 权限验证失败
     * @throws IllegalArgumentException PDF不存在
     */
    String getPdfPreviewUrlWithAuth(Integer detailId, HttpServletRequest request)
            throws SecurityException, IllegalArgumentException;
}
