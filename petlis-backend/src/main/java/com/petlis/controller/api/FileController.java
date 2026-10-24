package com.petlis.controller.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件访问控制器
 *
 * 提供上传文件的访问接口
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${app.file.upload-path:/data/petlis/uploads}")
    private String uploadPath;

    /**
     * 获取头像文件
     *
     * @param filename 文件名
     * @return 文件内容
     */
    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        log.debug("请求获取头像文件: {}", filename);

        try {
            // 构建文件路径
            Path filePath = Paths.get(uploadPath, "avatars", filename).normalize();
            File file = filePath.toFile();

            // 检查文件是否存在
            if (!file.exists() || !file.isFile()) {
                log.warn("文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // 检查文件路径是否在允许的目录内(防止路径遍历攻击)
            if (!filePath.startsWith(Paths.get(uploadPath).normalize())) {
                log.error("非法的文件访问路径: {}", filePath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 创建资源
            Resource resource = new FileSystemResource(file);

            // 确定文件类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.debug("返回文件: {}, type: {}", filename, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("读取文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
