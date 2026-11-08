package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

@Service @RequiredArgsConstructor
public class InvoiceFileService {
    private final FeatureStore store;
    private final ReviewPermission permission;
    private final PlatformFileService files;
    private final BusinessNotifications notifications;
    @Transactional
    public void issue(long id, MultipartFile upload) {
        permission.requireReviewer();
        var invoice=store.one("SELECT i.status,i.user_id,u.hospital_id FROM lin_invoice_application i JOIN lin_user u ON u.id=i.user_id WHERE i.id=? FOR UPDATE",id);
        if (!"pending".equals(invoice.get("status"))) throw new BusinessException(409,"发票申请已处理，请刷新状态");
        int owner=((Number)invoice.get("user_id")).intValue();
        int hospital=invoice.get("hospital_id")==null?0:((Number)invoice.get("hospital_id")).intValue();
        long fileId=files.saveInvoice(hospital,owner,id,upload);
        store.jdbc().update("UPDATE lin_invoice_application SET status='issued',invoice_url=?,issued_at=NOW() WHERE id=?", "/api/invoices/"+id+"/download",id);
        notifications.emit(owner,"invoice","issued:"+id,"电子发票已开具","开票申请 #"+id+" 已开具，可前往下载。","pages/invoices/invoices");
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,after_json) VALUES(?,'INVOICE_ISSUED','INVOICE_APPLICATION',?,?)",SecurityUtils.getCurrentUserId(),String.valueOf(id),store.encode(java.util.Map.of("fileId",fileId)));
    }
    @Transactional(readOnly=true)
    public ResponseEntity<Resource> download(long id) {
        var invoice=store.one("SELECT status,user_id FROM lin_invoice_application WHERE id=?",id);
        if (((Number)invoice.get("user_id")).intValue()!=SecurityUtils.getCurrentUserId() && !permission.isReviewer()) throw new BusinessException(403,"无权下载该发票");
        if (!"issued".equals(invoice.get("status"))) throw new BusinessException(404,"发票尚未开具");
        var file=store.one("SELECT id FROM lin_file WHERE biz_type='INVOICE' AND biz_id=? AND deleted_at IS NULL ORDER BY id DESC LIMIT 1",String.valueOf(id));
        return files.downloadInvoice(((Number)file.get("id")).longValue(),id);
    }
}
