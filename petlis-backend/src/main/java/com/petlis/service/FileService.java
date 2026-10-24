package com.petlis.service;

import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

/**
 * 文件服务接口
 * 提供文件上传、下载功能,支持本地文件系统和云存储
 *
 * @author PETLIS
 * @since 1.0.0
 */
public interface FileService {

    /**
     * 下载文件
     *
     * @param fileUrl 文件URL (可以是本地路径或云存储URL)
     * @return 文件内容
     */
    byte[] downloadFile(String fileUrl);

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file);

    /**
     * 上传文件到指定路径
     *
     * @param file     文件
     * @param filePath 目标路径
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String filePath);

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);

    /**
     * 判断文件是否存在
     *
     * @param fileUrl 文件URL
     * @return 是否存在
     */
    boolean fileExists(String fileUrl);

    /**
     * 获取文件URL
     *
     * @param filePath 文件路径
     * @return 文件访问URL
     */
    URL getFileUrl(String filePath);
}
