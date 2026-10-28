package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.dto.request.OrderCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.OptionalInt;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {
    private final FeatureStore store;
    private final AccessService access;
    private final ObjectMapper json;

    public OptionalInt begin(OrderCreateRequest request) {
        String key = key(request);
        if (key == null) return OptionalInt.empty();
        CurrentActor actor = access.current();
        int hospitalId = hospital(actor);
        String hash = requestHash(request);
        store.jdbc().update("INSERT INTO lin_order_idempotency(user_id,hospital_id,operation,idempotency_key,request_hash,status,expires_at) VALUES(?,?,'ORDER_CREATE',?,?,'PROCESSING',?) ON DUPLICATE KEY UPDATE id=id",
                actor.userId(), hospitalId, key, hash, LocalDateTime.now().plusHours(24));
        Map<String, Object> row = store.one("SELECT order_id,request_hash,status FROM lin_order_idempotency WHERE user_id=? AND operation=? AND idempotency_key=? FOR UPDATE", actor.userId(), "ORDER_CREATE", key);
        if (!hash.equals(row.get("request_hash"))) throw new BusinessException(409, "幂等键已用于不同的订单内容");
        if ("COMPLETED".equals(row.get("status")) && row.get("order_id") != null) return OptionalInt.of(number(row.get("order_id")).intValue());
        if ("PROCESSING".equals(row.get("status")) && row.get("order_id") != null) return OptionalInt.of(number(row.get("order_id")).intValue());
        return OptionalInt.empty();
    }

    public void complete(OrderCreateRequest request, int orderId, int hospitalId) {
        String key = key(request);
        CurrentActor actor = access.current();
        if (key != null) store.jdbc().update("UPDATE lin_order_idempotency SET order_id=?,response_body=?,status=?,completed_at=NOW(),response_code=200 WHERE user_id=? AND idempotency_key=?",
                orderId, store.encode(Map.of("orderId", orderId)), "COMPLETED", actor.userId(), key);
        store.jdbc().update("INSERT INTO lin_order_clinical(order_id,pet_id,hospital_id,medical_history,clinical_symptoms,diagnosis,medication,clinical_data,created_by) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE medical_history=VALUES(medical_history),clinical_symptoms=VALUES(clinical_symptoms),diagnosis=VALUES(diagnosis),medication=VALUES(medication),clinical_data=VALUES(clinical_data),updated_by=VALUES(created_by)",
                orderId, request.getPetId(), hospitalId, request.getMedicalHistory(), request.getClinicalSymptoms(), request.getDiagnosis(), request.getMedication(),
                store.encode(Map.of("source", "order-create")), actor.userId());
    }

    String requestHash(OrderCreateRequest request) {
        try {
            String value = json.writeValueAsString(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new BusinessException("订单幂等摘要生成失败"); }
    }

    private String key(OrderCreateRequest request) {
        if (request == null || request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) return null;
        String key = request.getIdempotencyKey().trim();
        if (key.length() > 128) throw new BusinessException("幂等键最多128个字符");
        return key;
    }

    private int hospital(CurrentActor actor) {
        if (actor.hospitalIds().size() != 1) throw new BusinessException("订单创建必须归属唯一医院");
        return actor.hospitalIds().iterator().next();
    }

    private Number number(Object value) { return (Number) value; }
}
