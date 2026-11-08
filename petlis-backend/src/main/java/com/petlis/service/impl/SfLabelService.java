package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.integration.sf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import lombok.RequiredArgsConstructor;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SfLabelService {
    private final FeatureStore store;
    private final AccessService access;
    private final PlatformFileService files;
    private final SfSandboxConfiguration configuration;

    public Map<String,Object> get(int orderId) {
        var shipment=shipment(orderId);
        var cached=cached(((Number)shipment.get("id")).longValue());
        if(cached==null) throw new BusinessException(404,"面单尚未生成，请联系管理员生成");
        return cached;
    }
    public Map<String,Object> generate(int orderId) {
        access.requireAdmin();
        var shipment=shipment(orderId);
        if("CANCELLED".equals(shipment.get("status"))) throw new BusinessException(409,"寄件已取消，不能生成面单");
        long id=((Number)shipment.get("id")).longValue();
        var cached=cached(id);if(cached!=null) return cached;
        byte[] content;
        try {
            var config=configuration.load();
            var outcome=config.gateway().call("COM_RECE_CLOUD_PRINT_WAYBILLS",Map.of("templateCode",config.templateCode(),"version","2.0","fileType","pdf","sync",true,"documents",List.of(Map.of("masterWaybillNo",shipment.get("tracking_no")))),UUID.randomUUID().toString(),System.currentTimeMillis());
            if(!outcome.success() || !config.templateCode().equals(outcome.data().path("templateCode").asText())) throw new IllegalStateException();
            // This endpoint requests exactly one parent label, never arbitrary or batch labels.
            var documents=outcome.data().path("files");
            if(documents.size()!=1) throw new IllegalStateException();
            var document=documents.get(0);
            if(!Objects.equals(shipment.get("tracking_no"),document.path("waybillNo").asText())) throw new IllegalStateException();
            content=new SfLabelDownloader(SfLabelDownloader.httpTransport()).download(document.path("url").asText(),document.path("token").asText());
        } catch(BusinessException e) {throw e;}
        catch(Exception e) {throw new BusinessException(409,"顺丰面单暂未生成，请稍后重试");}
        var transaction=new TransactionTemplate(new JdbcTransactionManager(Objects.requireNonNull(store.jdbc().getDataSource())));
        return transaction.execute(status->{
            var current=store.one("SELECT id,status FROM lin_shipment WHERE id=? AND is_current=1 FOR UPDATE",id);
            if("CANCELLED".equals(current.get("status"))) throw new BusinessException(409,"寄件已取消，不能生成面单");
            if(!store.jdbc().queryForList("SELECT id FROM lin_sf_request WHERE order_id=? AND attempt_no=? AND operation='CANCEL' AND state<>'REJECTED'",orderId,shipment.get("attempt_no")).isEmpty())
                throw new BusinessException(409,"已申请取消寄件，面单暂不可用");
            var existing=cached(id);if(existing!=null) return existing;
            var saved=files.saveShippingLabel(((Number)shipment.get("hospital_id")).intValue(),((Number)shipment.get("laboratory_id")).intValue(),id,content);
            return view(saved.id());
        });
    }
    private Map<String,Object> shipment(int orderId) {
        var row=store.one("SELECT s.* FROM lin_shipment s JOIN lin_sf_request r ON r.order_id=s.order_id AND r.attempt_no=s.attempt_no AND r.tracking_no=s.tracking_no AND r.operation='CREATE' AND r.state='SUCCEEDED' WHERE s.order_id=? AND s.is_current=1",orderId);
        access.requireHospital(((Number)row.get("hospital_id")).intValue());
        if(!store.jdbc().queryForList("SELECT id FROM lin_sf_request WHERE order_id=? AND attempt_no=? AND operation='CANCEL' AND state<>'REJECTED'",orderId,row.get("attempt_no")).isEmpty())
            throw new BusinessException(409,"已申请取消寄件，面单暂不可用");
        return row;
    }
    private Map<String,Object> cached(long shipmentId) {
        var rows=store.jdbc().queryForList("SELECT id FROM lin_file WHERE biz_type='SF_LABEL' AND biz_id=? AND deleted_at IS NULL ORDER BY id LIMIT 1",Long.toString(shipmentId));
        return rows.isEmpty()?null:view(((Number)rows.get(0).get("id")).longValue());
    }
    private Map<String,Object> view(long id) {return Map.of("id",id,"url","/platform-files/"+id+"/download","sandbox",true);}
}
