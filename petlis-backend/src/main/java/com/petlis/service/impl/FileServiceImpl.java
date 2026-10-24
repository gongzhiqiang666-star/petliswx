package com.petlis.service.impl;

import com.petlis.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件服务实现类
 * 支持本地文件系统存储
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Value("${app.file.upload-path:/data/petlis/uploads}")
    private String uploadPath;

    @Value("${app.file.access-url:http://localhost:8080/api/files}")
    private String accessUrl;

    /**
     * 下载文件
     *
     * @param fileUrl 文件URL (可以是本地路径或云存储URL)
     * @return 文件内容
     */
    @Override
    public byte[] downloadFile(String fileUrl) {
        try {
            // 判断是本地路径还是远程URL
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                // 远程URL下载
                return downloadFromUrl(fileUrl);
            } else {
                // 本地文件读取
                return readLocalFile(fileUrl);
            }
        } catch (Exception e) {
            log.error("下载文件失败, fileUrl: {}", fileUrl, e);
            throw new RuntimeException("下载文件失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件访问URL
     */
    @Override
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    /**
     * 上传文件到指定路径
     *
     * @param file     文件
     * @param filePath 目标路径
     * @return 文件访问URL
     */
    @Override
    public String uploadFile(MultipartFile file, String filePath) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                throw new IllegalArgumentException("文件不能为空");
            }

            // 获取原始文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + extension;

            // 构建目标路径
            Path targetPath;
            if (filePath != null && !filePath.isEmpty()) {
                targetPath = Paths.get(uploadPath, filePath, fileName);
            } else {
                targetPath = Paths.get(uploadPath, fileName);
            }

            // 确保目录存在
            Files.createDirectories(targetPath.getParent());

            // 保存文件
            file.transferTo(targetPath.toFile());

            log.info("文件上传成功, fileName: {}, targetPath: {}", fileName, targetPath);

            // 返回访问URL
            String relativePath = targetPath.toString().replace(uploadPath, "");
            return accessUrl + relativePath.replace("\\", "/");

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            // 从URL中提取文件路径
            String filePath = extractFilePath(fileUrl);

            if (filePath == null) {
                log.warn("无法从URL中提取文件路径: {}", fileUrl);
                return false;
            }

            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("文件删除成功: {}", path);
                return true;
            } else {
                log.warn("文件不存在: {}", path);
                return false;
            }
        } catch (Exception e) {
            log.error("删除文件失败, fileUrl: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param fileUrl 文件URL
     * @return 是否存在
     */
    @Override
    public boolean fileExists(String fileUrl) {
        try {
            String filePath = extractFilePath(fileUrl);
            if (filePath == null) {
                return false;
            }
            return Files.exists(Paths.get(filePath));
        } catch (Exception e) {
            log.error("检查文件存在性失败, fileUrl: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * 获取文件URL
     *
     * @param filePath 文件路径
     * @return 文件访问URL
     */
    @Override
    public URL getFileUrl(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                return file.toURI().toURL();
            }
            return null;
        } catch (MalformedURLException e) {
            log.error("生成文件URL失败, filePath: {}", filePath, e);
            return null;
        }
    }

    /**
     * 从本地读取文件
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 读取异常
     */
    private byte[] readLocalFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }
        return Files.readAllBytes(path);
    }

    /**
     * 从远程URL下载文件
     *
     * @param urlString URL字符串
     * @return 文件内容
     * @throws IOException 下载异常
     */
    private byte[] downloadFromUrl(String urlString) throws IOException {
        java.net.URL url = new java.net.URL(urlString);
        try (java.io.InputStream in = url.openStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * 从URL中提取文件路径
     *
     * @param fileUrl 文件URL
     * @return 文件路径
     */
    private String extractFilePath(String fileUrl) {
        try {
            // 如果是本地路径,直接返回
            if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
                return fileUrl;
            }

            // 从URL中提取路径
            URL url = new URL(fileUrl);
            String path = url.getPath();

            // 移除访问URL前缀
            if (accessUrl != null && path.startsWith(accessUrl.replace(url.getProtocol() + "://" + url.getHost(), ""))) {
                path = path.substring(accessUrl.length());
            }

            // 构建完整路径
            return uploadPath + path.replace("/", File.separator);
        } catch (Exception e) {
            log.error("提取文件路径失败, fileUrl: {}", fileUrl, e);
            return null;
        }
    }
}
