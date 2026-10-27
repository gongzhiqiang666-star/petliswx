package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.PlatformRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PlatformFileService {
    private static final Set<String> ALLOWED = Set.of("application/pdf", "image/png", "image/jpeg");
    private final FeatureStore store;
    private final AccessService access;
    private final Path root;
    private final long maxBytes;

    public PlatformFileService(FeatureStore store, AccessService access,
            @Value("${app.pdf.storage-path:./data/reports}") String root,
            @Value("${app.file.max-bytes:10485760}") long maxBytes) {
        this.store = store;
        this.access = access;
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public record Stored(long id, String url) { }

    public Stored saveReport(int hospitalId, int laboratoryId, long workflowId, byte[] content) {
        validate(content, "application/pdf", Math.min(maxBytes, 10 * 1024 * 1024));
        var actor = access.current();
        Integer uploadedBy = actor == null ? 0 : actor.userId();
        return save(hospitalId, laboratoryId, "REPORT", String.valueOf(workflowId), "report-" + workflowId + ".pdf", "application/pdf", content, uploadedBy);
    }

    public Map<String, Object> upload(MultipartFile upload, String bizType, String bizId) {
        var actor = access.current();
        String type = bizType == null || bizType.isBlank() ? "GENERAL" : bizType.toUpperCase();
        if (!Set.of("GENERAL", "QUALIFICATION").contains(type)) throw new BusinessException("文件业务类型无效");
        if ("QUALIFICATION".equals(type) && !actor.isAdmin() && !actor.hasRole(PlatformRole.HOSPITAL_ADMIN)) throw new BusinessException(403, "无权上传资质文件");
        byte[] content = bytes(upload);
        String mime = upload.getContentType();
        if (!ALLOWED.contains(mime)) throw new BusinessException("仅支持PDF、PNG和JPEG文件");
        validate(content, mime, maxBytes);
        int hospitalId = actor.hospitalIds().stream().findFirst().orElse(0);
        int laboratoryId = actor.laboratoryIds().stream().findFirst().orElse(0);
        String original = upload.getOriginalFilename() == null ? "file" : upload.getOriginalFilename().replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1);
        Stored stored = save(hospitalId, laboratoryId, type, bizId == null ? "0" : bizId, original, mime, content, actor.userId());
        return Map.of("id", stored.id(), "url", "/files/platform/" + stored.id() + "/download");
    }

    public ResponseEntity<Resource> download(long fileId) {
        Map<String, Object> file = store.one("SELECT * FROM lin_file WHERE id=? AND deleted_at IS NULL", fileId);
        if ("REPORT".equals(file.get("biz_type"))) throw new BusinessException(404, "报告文件必须通过报告权限访问");
        var actor = access.current();
        if (!actor.isAdmin()) {
            if (file.get("owner_user_id") != null && ((Number) file.get("owner_user_id")).intValue() == actor.userId()) return response(file);
            access.requireHospital(((Number) file.get("hospital_id")).intValue());
        }
        return response(file);
    }

    public ResponseEntity<Resource> downloadReport(long fileId, long workflowId) {
        Map<String, Object> file = store.one("SELECT * FROM lin_file WHERE id=? AND biz_type='REPORT' AND biz_id=? AND deleted_at IS NULL", fileId, String.valueOf(workflowId));
        return response(file);
    }

    private Stored save(int hospitalId, int laboratoryId, String bizType, String bizId, String originalName,
            String mime, byte[] content, Integer ownerUserId) {
        String storedName = UUID.randomUUID() + extension(mime);
        Path target = safe(storedName);
        try {
            Files.createDirectories(root);
            Files.write(target, content);
            long id = store.insert("INSERT INTO lin_file(hospital_id,laboratory_id,owner_user_id,biz_type,biz_id,original_name,storage_path,mime_type,file_size,sha256,visibility,uploaded_by) VALUES(?,?,?,?,?,?,?,?,?,?,'PRIVATE',?)",
                    hospitalId == 0 ? null : hospitalId, laboratoryId == 0 ? null : laboratoryId, ownerUserId, bizType, bizId,
                    originalName, storedName, mime, content.length, sha256(content), ownerUserId);
            return new Stored(id, "/Content/FilePdf/" + storedName);
        } catch (IOException error) {
            throw new BusinessException(500, "文件保存失败", error);
        }
    }

    private ResponseEntity<Resource> response(Map<String, Object> file) {
        try {
            Path path = safe(String.valueOf(file.get("storage_path")));
            if (!Files.isRegularFile(path)) throw new BusinessException(404, "文件不存在");
            if (!path.toRealPath().startsWith(root.toRealPath())) throw new BusinessException(403, "文件路径超出允许目录");
            Resource resource = new UrlResource(path.toUri());
            String filename = String.valueOf(file.get("original_name")).replaceAll("[\\r\\n\\\"]", "_");
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(String.valueOf(file.get("mime_type"))))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(Files.size(path)).body(resource);
        } catch (MalformedURLException error) { throw new BusinessException(404, "文件不存在"); }
          catch (IOException error) { throw new BusinessException(500, "文件读取失败", error); }
    }

    private Path safe(String name) {
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root) || path.equals(root) || name.contains("/") || name.contains("\\")) throw new BusinessException("文件路径不安全");
        return path;
    }

    private void validate(byte[] content, String mime, long limit) {
        if (content == null || content.length == 0 || content.length > limit || !matchesMagic(content, mime)) throw new BusinessException("文件魔数、类型或大小不符合要求");
    }

    private boolean matchesMagic(byte[] content, String mime) {
        if ("application/pdf".equals(mime)) return startsWith(content, new byte[]{'%', 'P', 'D', 'F', '-'});
        if ("image/png".equals(mime)) return startsWith(content, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
        if ("image/jpeg".equals(mime)) return startsWith(content, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        return false;
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if (content[i] != signature[i]) return false;
        return true;
    }

    private String extension(String mime) { return "application/pdf".equals(mime) ? ".pdf" : "image/png".equals(mime) ? ".png" : ".jpg"; }
    private byte[] bytes(MultipartFile upload) { try { return upload.getBytes(); } catch (IOException error) { throw new BusinessException("文件读取失败"); } }
    private String sha256(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
