package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Anonymous operational counts: no identity, clinical data, URL or free-text fields. */
@Service
@RequiredArgsConstructor
public class ClientEventService {
    private final FeatureStore store;
    private final AccessService access;
    private static final Set<String> FIELDS = Set.of("id", "event", "step", "outcome", "environment");
    private static final Map<String, Set<String>> STEPS = Map.of(
        "order_step", Set.of("pet", "tests", "sample", "confirm"),
        "order_submit", Set.of("confirm"), "payment_result", Set.of("checkout"),
        "report_open", Set.of("report"), "registration_submit", Set.of("registration"),
        "auth_result", Set.of("login"));
    private static final Map<String, Set<String>> OUTCOMES = Map.of(
        "order_step", Set.of("view"), "order_submit", Set.of("success", "failure"),
        "payment_result", Set.of("success", "cancelled", "failure", "pending"),
        "report_open", Set.of("success", "failure"), "registration_submit", Set.of("success", "failure"),
        "auth_result", Set.of("success", "failure"));

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void receive(List<Map<String, String>> events) {
        if (events == null || events.isEmpty() || events.size() > 20) throw invalid();
        // Validate the entire batch before writing anything, including unknown/private fields.
        for (var event : events) {
            if (event == null || !event.keySet().equals(FIELDS) || event.values().stream().anyMatch(Objects::isNull)) throw invalid();
            String name = event.get("event");
            if (event.get("id") == null || !event.get("id").matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) throw invalid();
            if (!STEPS.getOrDefault(name, Set.of()).contains(event.get("step")) || !OUTCOMES.getOrDefault(name, Set.of()).contains(event.get("outcome"))) throw invalid();
            if (!Set.of("develop", "trial", "release").contains(event.get("environment"))) throw invalid();
        }
        store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE", access.current().userId());
        for (var event : events) {
            // Indexed current/gap lock prevents concurrent replay insertion; deadlocks roll back and client retries.
            if (!store.jdbc().queryForList("SELECT id FROM lin_audit_record WHERE resource_type='MINIPROGRAM_EVENT' AND resource_id=? FOR UPDATE", event.get("id")).isEmpty()) continue;
            var attributes = new LinkedHashMap<>(event); attributes.remove("id");
            store.insert("INSERT INTO lin_audit_record(action,resource_type,resource_id,after_json) VALUES('CLIENT_EVENT','MINIPROGRAM_EVENT',?,?)", event.get("id"), store.encode(attributes));
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> summary(int days) {
        access.requireAdmin();
        if (days < 1 || days > 30) throw new BusinessException(400, "统计范围为1至30天");
        return store.jdbc().queryForList("SELECT DATE(created_at) AS day,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.event')) AS event,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.step')) AS step,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.outcome')) AS outcome,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.environment')) AS environment,COUNT(*) AS count FROM lin_audit_record WHERE resource_type='MINIPROGRAM_EVENT' AND action='CLIENT_EVENT' AND created_at>=DATE_SUB(NOW(),INTERVAL ? DAY) GROUP BY day,event,step,outcome,environment ORDER BY day DESC,event,step,outcome,environment", days);
    }
    private BusinessException invalid() { return new BusinessException(400, "事件格式或枚举无效"); }
}
