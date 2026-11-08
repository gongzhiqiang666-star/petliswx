package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Map;

/** User::CannelPos: legacy hospital order 0 -> 4, no refund or detail mutation. */
@Service @RequiredArgsConstructor
public class LegacyHospitalCancellationService {
    private final FeatureStore store;
    private final AccessService access;
    private final LegacyHospitalHistoryService history;
    public record Command(Integer hospitalId,String expectedToken,String requestKey) {}

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> cancel(long orderId,Command command) {
        if (command==null || command.hospitalId()==null || command.hospitalId()<=0
                || !access.current().canAccessHospital(command.hospitalId()))
            throw new BusinessException(403,"无权取消该医院送检单");
        if (!history.policy(true).get("legacyHospitalHistoryCancel"))
            throw new BusinessException(409,"原医院送检取消尚未开启");
        if (orderId<1 || orderId>Integer.MAX_VALUE || command.expectedToken()==null
                || !command.expectedToken().matches("[a-f0-9]{64}") || command.requestKey()==null
                || !command.requestKey().matches("[A-Za-z0-9_-]{16,64}"))
            throw new BusinessException(400,"请刷新送检历史后重试");
        int actor=access.current().userId();
        store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
        String canonical=store.encode(Map.of("orderId",orderId,"command",command));
        var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_CANCEL' AND request_id=? ORDER BY id DESC LIMIT 1",actor,command.requestKey());
        if (!prior.isEmpty()) {
            try {
                var saved=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());
                if (!canonical.equals(saved.path("canonicalRequest").asText())) throw new BusinessException(409,"取消请求号已用于不同内容");
                return Map.of("orderId",orderId,"sampleStatus",4);
            } catch (com.fasterxml.jackson.core.JsonProcessingException error) { throw new BusinessException(409,"取消请求记录无法核对"); }
        }
        var before=history.cancellationSnapshot(orderId,command.hospitalId(),true);
        if (LegacyHospitalHistoryService.hasNewWorkflow(before)) throw new BusinessException(409,"该单已接入新流程，请通过新流程操作");
        if (((Number)before.get("sampleStatus")).intValue()!=0) throw new BusinessException(409,"仅待审核的原医院送检单可取消");
        if (!history.cancellationToken(before).equals(command.expectedToken())) throw new BusinessException(409,"送检单已变更，请刷新后核对");
        int changed=store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=4 WHERE ListDiagnosisProjectId=? AND InfoHospitalId=? AND SampleStatus=0",orderId,command.hospitalId());
        if (changed!=1) throw new BusinessException(409,"送检单状态已变化，请刷新后重试");
        store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_HOSPITAL_CANCELLED','LEGACY_HOSPITAL_CANCEL',?,?,?,?)",actor,String.valueOf(orderId),command.requestKey(),store.encode(before),store.encode(Map.of("canonicalRequest",canonical,"sampleStatus",4,"financialChange",false)));
        return Map.of("orderId",orderId,"sampleStatus",4);
    }
}
