package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationalService {
    private final FeatureStore store;
    private final ReviewPermission permission;

    public Map<String, Object> configs(int page, int size) {
        permission.requireReviewer();
        return store.page("SELECT COUNT(*) FROM lin_operational_config", "SELECT * FROM lin_operational_config ORDER BY config_key", page, size);
    }

    public Map<String, Object> audits(String action, int page, int size) {
        permission.requireReviewer();
        if (action == null || action.isBlank()) return store.page("SELECT COUNT(*) FROM lin_audit_record", "SELECT id,actor_user_id AS operatorId,actor_role AS actorRole,action,resource_type AS targetType,resource_id AS targetId,reason,created_at AS createdAt FROM lin_audit_record ORDER BY id DESC", page, size);
        return store.page("SELECT COUNT(*) FROM lin_audit_record WHERE action=?", "SELECT id,actor_user_id AS operatorId,actor_role AS actorRole,action,resource_type AS targetType,resource_id AS targetId,reason,created_at AS createdAt FROM lin_audit_record WHERE action=? ORDER BY id DESC", page, size, action);
    }
    public Map<String, Object> audit(long id) {
        permission.requireReviewer();
        return store.one("SELECT id,actor_user_id AS operatorId,actor_role AS actorRole,action,resource_type AS targetType,resource_id AS targetId,before_json AS beforeSnapshot,after_json AS afterSnapshot,reason,created_at AS createdAt FROM lin_audit_record WHERE id=?", id);
    }

    @Transactional
    public void saveConfig(String key, Map<String, Object> value, int version, String reason) {
        permission.requireReviewer();
        if (key == null || !key.matches("[A-Za-z0-9_.:-]{2,128}") || value == null || value.isEmpty()) throw new BusinessException("运营配置无效");
        Map<String, Object> before = store.one("SELECT version,config_value FROM lin_operational_config WHERE config_key=? AND scope_type='GLOBAL' AND scope_id=0 FOR UPDATE", key);
        if (((Number) before.get("version")).intValue() != version) throw new BusinessException(409, "配置已变更，请刷新后重试");
        if (store.jdbc().update("UPDATE lin_operational_config SET config_value=?,version=version+1,updated_by=? WHERE config_key=? AND scope_type='GLOBAL' AND scope_id=0 AND version=?",
                store.encode(value), SecurityUtils.getCurrentUserId(), key, version) != 1) throw new BusinessException(409, "配置已变更，请刷新后重试");
        audit("OPERATIONAL_CONFIG_UPDATED", "OPERATIONAL_CONFIG", key, reason);
    }

    @Transactional
    public long createConfig(String key, Map<String, Object> value, String description) {
        permission.requireReviewer();
        if (key == null || !key.matches("[A-Za-z0-9_.:-]{2,128}") || value == null || value.isEmpty()) throw new BusinessException("运营配置无效");
        long id = store.insert("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES(?,?,?,?)", key, store.encode(value), description, SecurityUtils.getCurrentUserId());
        audit("OPERATIONAL_CONFIG_CREATED", "OPERATIONAL_CONFIG", key, description);
        return id;
    }

    @Transactional
    public void adjustPoints(int userId, int delta, String reason, String requestKey) {
        permission.requireReviewer();
        if (delta == 0 || reason == null || reason.isBlank() || reason.length() > 500 || requestKey == null || !requestKey.matches("[A-Za-z0-9_-]{8,100}")) throw new BusinessException("积分调整请求无效");
        var existing = store.jdbc().queryForList("SELECT id FROM lin_audit_record WHERE resource_type='POINTS_ADJUSTMENT' AND request_id=?", requestKey);
        if (!existing.isEmpty()) return;
        Map<String, Object> user = store.one("SELECT score FROM lin_user WHERE id=? FOR UPDATE", userId);
        existing = store.jdbc().queryForList("SELECT id FROM lin_audit_record WHERE resource_type='POINTS_ADJUSTMENT' AND request_id=?", requestKey);
        if (!existing.isEmpty()) return;
        int before = ((Number) user.get("score")).intValue();
        long after = (long) before + delta;
        if (after < 0 || after > Integer.MAX_VALUE) throw new BusinessException("调整后积分余额无效");
        store.jdbc().update("UPDATE lin_user SET score=?,updatetime=UNIX_TIMESTAMP() WHERE id=?", (int) after, userId);
        store.jdbc().update("INSERT INTO lin_user_score_log(user_id,score,`before`,`after`,memo,createtime) VALUES(?,?,?,?,?,UNIX_TIMESTAMP())", userId, delta, before, (int) after, reason);
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json,reason) VALUES(?,?,?,?,?,?,?)",
                SecurityUtils.getCurrentUserId(), "POINTS_ADJUSTED", "POINTS_ADJUSTMENT", String.valueOf(userId), requestKey, store.encode(Map.of("before", before, "after", (int) after)), reason);
    }

    private void audit(String action, String type, String id, String reason) {
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,reason) VALUES(?,?,?,?,?)", SecurityUtils.getCurrentUserId(), action, type, id, reason);
    }
}
