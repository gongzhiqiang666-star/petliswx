package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.PlatformRole;
import com.petlis.common.security.SecurityUtils;
import com.petlis.common.security.UserRoleAssignment;
import com.petlis.common.security.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final FeatureStore store;
    private final UserRoleService userRoles;
    private final SessionService sessions;
    private final ReviewService reviews;
    private final AccessService access;

    public Map<String, Object> users(String status, String role, Integer hospitalId, int page, int size) {
        requireAdmin();
        StringBuilder where = new StringBuilder(" FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=u.hospital_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendUserStatus(where, status);
        if (hospitalId != null) { where.append(" AND u.hospital_id=?"); args.add(hospitalId); }
        if (role != null && !role.isBlank()) {
            PlatformRole.valueOf(role);
            where.append(" AND EXISTS(SELECT 1 FROM lin_user_role r WHERE r.user_id=u.id AND r.is_active=1 AND r.role_code='").append(role).append("')");
        }
        String select = "SELECT u.id,u.username,u.nickname AS displayName,u.mobile AS phone,u.hospital_id AS hospitalId,h.HospitalName AS hospitalName,(u.openid IS NOT NULL AND u.openid<>'') AS wechatBound,u.status,p.review_status AS reviewStatus,(SELECT GROUP_CONCAT(r.role_code ORDER BY r.id) FROM lin_user_role r WHERE r.user_id=u.id AND r.is_active=1) AS roleCodes" + where + " ORDER BY u.id DESC";
        return withRoles(store.page("SELECT COUNT(*)" + where, select, page, size, args.toArray()));
    }

    public List<UserRoleAssignment> roles(int userId) {
        requireAdmin();
        store.one("SELECT id FROM lin_user WHERE id=?", userId);
        return userRoles.assignments(userId);
    }

    @Transactional
    public void assignRole(int userId, String roleCode, Integer hospitalId, Integer laboratoryId) {
        requireAdmin();
        store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE", userId);
        int operator = SecurityUtils.getCurrentUserId();
        userRoles.assign(userId, roleCode, hospitalId, laboratoryId, operator);
        sessions.revoke(userId);
        audit("ROLE_ASSIGNED", "USER", userId, roleCode);
    }

    @Transactional
    public void removeRole(int userId, long assignmentId) {
        requireAdmin();
        userRoles.remove(assignmentId, userId);
        sessions.revoke(userId);
        audit("ROLE_REMOVED", "USER", userId, String.valueOf(assignmentId));
    }

    @Transactional
    public void setUserStatus(int userId, boolean active, String reason) {
        requireAdmin();
        if (SecurityUtils.getCurrentUserId() == userId) throw new BusinessException(403, "不能停用当前管理员自己");
        if (!active && (reason == null || reason.isBlank() || reason.length() > 500)) throw new BusinessException("请填写有效停用原因");
        store.one("SELECT id,status FROM lin_user WHERE id=? FOR UPDATE", userId);
        if (store.jdbc().update(active ? "UPDATE lin_user SET status='normal',updatetime=UNIX_TIMESTAMP() WHERE id=?" : "UPDATE lin_user SET status='hidden',updatetime=UNIX_TIMESTAMP() WHERE id=?", userId) != 1) {
            throw new BusinessException(404, "用户不存在");
        }
        sessions.revoke(userId);
        audit(active ? "USER_ENABLED" : "USER_DISABLED", "USER", userId, reason);
    }

    @Transactional
    public void reviewUser(int userId, ReviewService.Decision decision) {
        requireAdmin();
        if (SecurityUtils.getCurrentUserId() == userId) throw new BusinessException(403, "管理员不能审核自己");
        reviews.registration(userId, decision);
    }

    public Map<String, Object> hospitals(String status, int page, int size) {
        requireAdmin();
        String normalized = status == null ? "ACTIVE" : status.toUpperCase();
        String condition = switch (normalized) {
            case "ALL" -> "1=1";
            case "ACTIVE" -> "h.IsActive=1";
            case "INACTIVE", "REJECTED" -> "h.IsActive=0";
            case "PENDING" -> "1=0";
            default -> throw new BusinessException("医院状态无效");
        };
        String from = " FROM lin_lb_infohospital h WHERE " + condition;
        return store.page("SELECT COUNT(*)" + from,
                "SELECT h.InfoHospitalId AS id,h.HospitalName AS name,h.LoginName AS code,h.CityId AS cityId,h.Tel AS contactPhone,h.Adress AS address,h.PayType AS payType,h.amount AS amount,CASE WHEN h.IsActive=1 THEN 'ACTIVE' ELSE 'INACTIVE' END AS status" + from + " ORDER BY h.HospitalName,h.InfoHospitalId",
                page, size);
    }

    public Map<String, Object> hospital(int hospitalId) {
        requireAdmin();
        return store.one("SELECT InfoHospitalId AS id,HospitalName AS name,LoginName AS code,CityId AS cityId,Tel AS contactPhone,Adress AS address,Place AS place,Invoice AS invoice,PayType AS payType,amount,CASE WHEN IsActive=1 THEN 'ACTIVE' ELSE 'INACTIVE' END AS status FROM lin_lb_infohospital WHERE InfoHospitalId=?", hospitalId);
    }

    @Transactional
    public void reviewHospital(int hospitalId, ReviewService.Decision decision) {
        requireAdmin();
        if (!decision.approved() && (decision.reason() == null || decision.reason().isBlank() || decision.reason().length() > 500)) {
            throw new BusinessException("请填写有效驳回原因");
        }
        store.one("SELECT InfoHospitalId,IsActive FROM lin_lb_infohospital WHERE InfoHospitalId=? FOR UPDATE", hospitalId);
        store.jdbc().update("UPDATE lin_lb_infohospital SET IsActive=? WHERE InfoHospitalId=?", decision.approved() ? 1 : 0, hospitalId);
        store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,review_status,credential_version) SELECT id,'approved',1 FROM lin_user WHERE hospital_id=? ON DUPLICATE KEY UPDATE credential_version=credential_version+1", hospitalId);
        audit(decision.approved() ? "HOSPITAL_APPROVED" : "HOSPITAL_REJECTED", "HOSPITAL", hospitalId, decision.reason());
    }

    private void appendUserStatus(StringBuilder where, String status) {
        String normalized = status == null ? "ACTIVE" : status.toUpperCase();
        switch (normalized) {
            case "ALL" -> { }
            case "ACTIVE" -> where.append(" AND u.status='normal' AND (p.review_status='approved' OR p.user_id IS NULL)");
            case "INACTIVE" -> where.append(" AND u.status='hidden' AND (p.review_status IS NULL OR p.review_status='approved')");
            case "PENDING" -> where.append(" AND p.review_status='pending'");
            case "REJECTED" -> where.append(" AND p.review_status='rejected'");
            default -> throw new BusinessException("用户状态无效");
        }
    }

    private Map<String, Object> withRoles(Map<String, Object> page) {
        List<Map<String, Object>> items = ((List<Map<String, Object>>) page.get("items")).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>(row);
            Object codes = item.remove("roleCodes");
            List<String> roleList = codes == null || codes.toString().isBlank() ? List.of() : Arrays.asList(codes.toString().split(","));
            item.put("roles", roleList);
            Object legacyStatus = item.remove("status");
            Object reviewStatus = item.get("reviewStatus");
            item.put("status", "rejected".equals(reviewStatus) ? "REJECTED" : "pending".equals(reviewStatus) ? "PENDING" : "normal".equals(legacyStatus) ? "ACTIVE" : "INACTIVE");
            return item;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>(page);
        result.put("items", items);
        return result;
    }

    private void requireAdmin() {
        access.requireAdmin();
    }

    private void audit(String action, String resourceType, Object resourceId, String reason) {
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,reason) VALUES(?,?,?,?,?)",
                SecurityUtils.getCurrentUserId(), action, resourceType, String.valueOf(resourceId), reason);
    }
}
