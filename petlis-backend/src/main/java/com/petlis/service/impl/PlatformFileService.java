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
@lombok.extern.slf4j.Slf4j
public class PlatformFileService {
    private static final Set<String> ALLOWED = Set.of("application/pdf", "image/png", "image/jpeg");
    private final FeatureStore store;
    private final AccessService access;
    private final com.petlis.common.security.ReviewPermission reviewer;
    private final Path root;
    private final long maxBytes;

    public PlatformFileService(FeatureStore store, AccessService access, com.petlis.common.security.ReviewPermission reviewer,
            @Value("${app.pdf.storage-path:./data/reports}") String root,
            @Value("${app.file.max-bytes:10485760}") long maxBytes) {
        this.store = store;
        this.access = access;
        this.reviewer = reviewer;
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

    public Stored saveShippingLabel(int hospitalId,int laboratoryId,long shipmentId,byte[] content) {
        access.requireAdmin();
        validate(content,"application/pdf",Math.min(maxBytes,10*1024*1024));
        return save(hospitalId,laboratoryId,"SF_LABEL",String.valueOf(shipmentId),"sf-sandbox-label-"+shipmentId+".pdf","application/pdf",content,access.current().userId());
    }

    public long saveInvoice(int hospitalId,int ownerUserId,long applicationId,MultipartFile upload) {
        reviewer.requireReviewer();
        byte[] content=bytes(upload);
        if (!"application/pdf".equals(upload.getContentType())) throw new BusinessException("电子发票必须为PDF");
        validate(content,"application/pdf",Math.min(maxBytes,10*1024*1024));
        Stored saved=save(hospitalId,0,"INVOICE",String.valueOf(applicationId),"invoice-"+applicationId+".pdf","application/pdf",content,ownerUserId);
        store.jdbc().update("UPDATE lin_file SET uploaded_by=? WHERE id=?",access.current().userId(),saved.id());
        return saved.id();
    }
    public ResponseEntity<Resource> downloadInvoice(long fileId,long applicationId) {
        return response(store.one("SELECT * FROM lin_file WHERE id=? AND biz_type='INVOICE' AND biz_id=? AND deleted_at IS NULL",fileId,String.valueOf(applicationId)));
    }

    public long saveHospitalDocument(int hospitalId,MultipartFile upload) {
        access.requireAdmin();
        return saveHospitalFile(hospitalId,upload,"HOSPITAL_DOCUMENT");
    }
    public long saveHospitalSubmission(int hospitalId,MultipartFile upload) {
        access.requireHospitalAdmin(hospitalId);
        return saveHospitalFile(hospitalId,upload,"HOSPITAL_DOCUMENT_REVIEW");
    }
    private long saveHospitalFile(int hospitalId,MultipartFile upload,String type) {
        if(hospitalId<=0)throw new BusinessException(400,"医院编号无效");
        byte[] content=bytes(upload);String mime=upload.getContentType();
        if(!ALLOWED.contains(mime))throw new BusinessException(400,"医院资料仅支持PDF、PNG或JPEG");
        validate(content,mime,Math.min(maxBytes,10*1024*1024));
        String original=upload.getOriginalFilename()==null?"document":upload.getOriginalFilename().replace('\\','/');
        original=original.substring(original.lastIndexOf('/')+1).replaceAll("[\\r\\n\\x00]","_");
        if(original.isBlank() || original.length()>200)throw new BusinessException(400,"资料文件名需为1至200字符");
        return save(hospitalId,0,type,String.valueOf(hospitalId),original,mime,content,access.current().userId()).id();
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
        if ("REPORT".equals(file.get("biz_type")) || "INVOICE".equals(file.get("biz_type"))) throw new BusinessException(404, "报告文件必须通过报告权限访问");
        var actor = access.current();
        if(Set.of("HOSPITAL_DOCUMENT","HOSPITAL_DOCUMENT_REVIEW").contains(file.get("biz_type"))){access.requireHospitalAdmin(((Number)file.get("hospital_id")).intValue());return response(file);}
        if("SF_LABEL".equals(file.get("biz_type"))) {
            access.requireHospital(((Number)file.get("hospital_id")).intValue());
            var shipment=store.one("SELECT s.status,s.is_current,EXISTS(SELECT 1 FROM lin_sf_request r WHERE r.order_id=s.order_id AND r.attempt_no=s.attempt_no AND r.operation='CANCEL' AND r.state<>'REJECTED') AS cancelling FROM lin_shipment s WHERE s.id=?",file.get("biz_id"));
            if(shipment.containsKey("is_current") && !Integer.valueOf(1).equals(shipment.get("is_current")) || "CANCELLED".equals(shipment.get("status")) || Boolean.TRUE.equals(shipment.get("cancelling")) || (shipment.get("cancelling") instanceof Number n && n.intValue()!=0))
                throw new BusinessException(409,"寄件已取消或取消结果待确认，面单暂不可用");
            return response(file);
        }
        if ("QUALIFICATION".equals(file.get("biz_type"))) {
            if (actor.isAdmin() || reviewer.isReviewer()) return response(file);
            if (file.get("owner_user_id") != null && ((Number) file.get("owner_user_id")).intValue() == actor.userId()) return response(file);
            throw new BusinessException(403, "仅本人或授权审核人员可下载资质文件");
        }
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
        boolean created=false;
        try {
            Files.createDirectories(root);
            try(var output=Files.newOutputStream(target,java.nio.file.StandardOpenOption.CREATE_NEW,java.nio.file.StandardOpenOption.WRITE)){created=true;output.write(content);}
            long id = store.insert("INSERT INTO lin_file(hospital_id,laboratory_id,owner_user_id,biz_type,biz_id,original_name,storage_path,mime_type,file_size,sha256,visibility,uploaded_by) VALUES(?,?,?,?,?,?,?,?,?,?,'PRIVATE',?)",
                    hospitalId == 0 ? null : hospitalId, laboratoryId == 0 ? null : laboratoryId, ownerUserId, bizType, bizId,
                    originalName, storedName, mime, content.length, sha256(content), ownerUserId);
            if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
                    @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)discard(target);}
                });
            return new Stored(id, "/Content/FilePdf/" + storedName);
        } catch (IOException error) {
            if(created)discard(target);
            throw new BusinessException(500, "文件保存失败", error);
        } catch (RuntimeException error) {
            if(created)discard(target);throw error;
        }
    }

    private void discard(Path target){try{Files.deleteIfExists(target);}catch(IOException cleanup){log.warn("本次失败上传文件清理未完成: {}",target.getFileName());}}

    private ResponseEntity<Resource> response(Map<String, Object> file) {
        try {
            Path path = safe(String.valueOf(file.get("storage_path")));
            if (!Files.isRegularFile(path)) throw new BusinessException(404, "文件不存在");
            if (!path.toRealPath().startsWith(root.toRealPath())) throw new BusinessException(403, "文件路径超出允许目录");
            Resource resource = new UrlResource(path.toUri());
            String filename = String.valueOf(file.get("original_name")).replaceAll("[\\r\\n\\\"]", "_");
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(String.valueOf(file.get("mime_type"))))
                    .header(HttpHeaders.CONTENT_DISPOSITION, org.springframework.http.ContentDisposition.attachment().filename(filename,java.nio.charset.StandardCharsets.UTF_8).build().toString())
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
