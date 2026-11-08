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
        return store.page("SELECT COUNT(*) FROM lin_operational_config WHERE scope_type='GLOBAL' AND scope_id=0", "SELECT * FROM lin_operational_config WHERE scope_type='GLOBAL' AND scope_id=0 ORDER BY config_key", page, size);
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
        configAudit("OPERATIONAL_CONFIG_UPDATED",key,snapshot(version,decodeConfig(before.get("config_value"))),snapshot(version+1,value),reason);
    }

    @Transactional
    public long createConfig(String key, Map<String, Object> value, String description) {
        permission.requireReviewer();
        if (key == null || !key.matches("[A-Za-z0-9_.:-]{2,128}") || value == null || value.isEmpty()) throw new BusinessException("运营配置无效");
        long id = store.insert("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES(?,?,?,?)", key, store.encode(value), description, SecurityUtils.getCurrentUserId());
        configAudit("OPERATIONAL_CONFIG_CREATED",key,null,snapshot(0,value),description);
        return id;
    }

    public Map<String,Object> configHistory(String key,int page,int size) {
        permission.requireReviewer();
        validateConfigKey(key);
        var current=store.one("SELECT config_key,config_value,version FROM lin_operational_config WHERE config_key=? AND scope_type='GLOBAL' AND scope_id=0",key);
        String where=" FROM lin_audit_record WHERE resource_type='OPERATIONAL_CONFIG' AND resource_id=? AND action IN ('OPERATIONAL_CONFIG_CREATED','OPERATIONAL_CONFIG_UPDATED','OPERATIONAL_CONFIG_RESTORED')";
        return Map.of("current",current,"history",store.page("SELECT COUNT(*)"+where,"SELECT id,action,actor_user_id AS operatorId,before_json AS beforeSnapshot,after_json AS afterSnapshot,reason,created_at AS createdAt"+where+" ORDER BY id DESC",page,size,key));
    }
    @Transactional
    public void restoreConfig(String key,long auditId,int version,String reason) {
        restoreConfig(key,auditId,version,reason,"AFTER");
    }
    @Transactional
    public void restoreConfig(String key,long auditId,int version,String reason,String snapshotSide) {
        permission.requireReviewer();
        if(snapshotSide==null) snapshotSide="AFTER";
        if(!List.of("BEFORE","AFTER").contains(snapshotSide)) throw new BusinessException(400,"配置快照位置无效");
        validateConfigKey(key);
        if(auditId<1 || reason==null || reason.isBlank() || reason.length()>500) throw new BusinessException(400,"恢复配置须选择历史记录并填写500字以内原因");
        var before=store.one("SELECT version,config_value FROM lin_operational_config WHERE config_key=? AND scope_type='GLOBAL' AND scope_id=0 FOR UPDATE",key);
        if(((Number)before.get("version")).intValue()!=version) throw new BusinessException(409,"配置已变更，请刷新后重试");
        var history=store.one("SELECT before_json,after_json FROM lin_audit_record WHERE id=? AND resource_type='OPERATIONAL_CONFIG' AND resource_id=? AND action IN ('OPERATIONAL_CONFIG_CREATED','OPERATIONAL_CONFIG_UPDATED','OPERATIONAL_CONFIG_RESTORED') FOR UPDATE",auditId,key);
        var source=decodeConfig(history.get("BEFORE".equals(snapshotSide)?"before_json":"after_json"));
        if(!"GLOBAL".equals(source.get("scopeType")) || !(source.get("scopeId") instanceof Number id) || id.longValue()!=0 || !(source.get("version") instanceof Number) || !(source.get("value") instanceof Map<?,?> raw) || raw.isEmpty())
            throw new BusinessException(409,"该历史记录没有可恢复的全局配置快照");
        @SuppressWarnings("unchecked") Map<String,Object> value=(Map<String,Object>)source.get("value");
        if(store.jdbc().update("UPDATE lin_operational_config SET config_value=?,version=version+1,updated_by=? WHERE config_key=? AND scope_type='GLOBAL' AND scope_id=0 AND version=?",store.encode(value),SecurityUtils.getCurrentUserId(),key,version)!=1)
            throw new BusinessException(409,"配置已变更，请刷新后重试");
        var after=new java.util.LinkedHashMap<String,Object>(snapshot(version+1,value));
        after.put("restoredFromAuditId",auditId);
        after.put("restoredSnapshotSide",snapshotSide);
        configAudit("OPERATIONAL_CONFIG_RESTORED",key,snapshot(version,decodeConfig(before.get("config_value"))),after,reason);
    }
    private static void validateConfigKey(String key) {
        if(key==null || !key.matches("[A-Za-z0-9_.:-]{2,128}")) throw new BusinessException(400,"运营配置键无效");
    }
    private Map<String,Object> decodeConfig(Object raw) {
        try {
            if(raw==null) throw new IllegalArgumentException();
            var node=new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw.toString());
            if(!node.isObject()) throw new IllegalArgumentException();
            return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(node,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
        } catch(Exception error) { throw new BusinessException(409,"配置快照缺失或格式无效，未执行恢复"); }
    }
    private Map<String,Object> snapshot(int version,Map<String,Object> value) {
        return Map.of("version",version,"scopeType","GLOBAL","scopeId",0,"value",value);
    }
    private void configAudit(String action,String key,Map<String,Object> before,Map<String,Object> after,String reason) {
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,before_json,after_json,reason) VALUES(?,?,?,?,?,?,?)",SecurityUtils.getCurrentUserId(),action,"OPERATIONAL_CONFIG",key,before==null?null:store.encode(before),store.encode(after),reason);
    }

    @Transactional
    public void adjustPoints(int userId, int delta, String reason, String requestKey) {
        permission.requireReviewer();
        if (delta == 0 || reason == null || reason.isBlank() || reason.length() > 500 || requestKey == null || !requestKey.matches("[A-Za-z0-9_-]{8,100}")) throw new BusinessException("积分调整请求无效");
        // A request key belongs to its authenticated operator, including concurrent different-target calls.
        int operatorId = SecurityUtils.getCurrentUserId();
        store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE", operatorId);
        var existing = store.jdbc().queryForList("SELECT id,actor_user_id,resource_id,reason,CAST(JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.after')) AS SIGNED)-CAST(JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.before')) AS SIGNED) AS delta FROM lin_audit_record WHERE resource_type='POINTS_ADJUSTMENT' AND request_id=? AND actor_user_id=? FOR UPDATE", requestKey, operatorId);
        if (!existing.isEmpty()) { validatePointsReplay(existing.get(0), userId, delta, reason); return; }
        Map<String, Object> user = store.one("SELECT score FROM lin_user WHERE id=? FOR UPDATE", userId);
        existing = store.jdbc().queryForList("SELECT id,actor_user_id,resource_id,reason,CAST(JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.after')) AS SIGNED)-CAST(JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.before')) AS SIGNED) AS delta FROM lin_audit_record WHERE resource_type='POINTS_ADJUSTMENT' AND request_id=? AND actor_user_id=? FOR UPDATE", requestKey, operatorId);
        if (!existing.isEmpty()) { validatePointsReplay(existing.get(0), userId, delta, reason); return; }
        int before = ((Number) user.get("score")).intValue();
        long after = (long) before + delta;
        if (after < 0 || after > Integer.MAX_VALUE) throw new BusinessException("调整后积分余额无效");
        store.jdbc().update("UPDATE lin_user SET score=?,updatetime=UNIX_TIMESTAMP() WHERE id=?", (int) after, userId);
        store.jdbc().update("INSERT INTO lin_user_score_log(user_id,score,`before`,`after`,memo,createtime) VALUES(?,?,?,?,?,UNIX_TIMESTAMP())", userId, delta, before, (int) after, reason);
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json,reason) VALUES(?,?,?,?,?,?,?)",
                SecurityUtils.getCurrentUserId(), "POINTS_ADJUSTED", "POINTS_ADJUSTMENT", String.valueOf(userId), requestKey, store.encode(Map.of("before", before, "after", (int) after)), reason);
    }

    private void validatePointsReplay(Map<String,Object> prior, int userId, int delta, String reason) {
        if (!String.valueOf(userId).equals(String.valueOf(prior.get("resource_id")))
                || !(prior.get("delta") instanceof Number) || ((Number)prior.get("delta")).longValue() != delta
                || !reason.equals(prior.get("reason")) || !(prior.get("actor_user_id") instanceof Number)
                || ((Number)prior.get("actor_user_id")).intValue() != SecurityUtils.getCurrentUserId()) {
            throw new BusinessException(409,"该积分请求号已用于不同调整，请刷新并使用新请求号");
        }
    }

    private void audit(String action, String type, String id, String reason) {
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,reason) VALUES(?,?,?,?,?)", SecurityUtils.getCurrentUserId(), action, type, id, reason);
    }
}
