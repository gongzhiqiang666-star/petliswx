package com.petlis.common.security;

import com.petlis.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRoleService {
    private final JdbcTemplate jdbc;

    public List<UserRoleAssignment> assignments(int userId) {
        return jdbc.queryForList("SELECT id,user_id,role_code,hospital_id,laboratory_id,scope_type,scope_id FROM lin_user_role WHERE user_id=? AND is_active=1 ORDER BY id", userId)
                .stream().map(this::assignment).toList();
    }

    public Set<PlatformRole> roles(int userId) {
        Set<PlatformRole> result = new LinkedHashSet<>();
        for (UserRoleAssignment assignment : assignments(userId)) result.add(assignment.role());
        return result;
    }

    @Transactional
    public void assign(int userId, String roleCode, Integer hospitalId, Integer laboratoryId, int operatorId) {
        PlatformRole role = role(roleCode);
        String scopeType;
        int scopeId;
        switch (role) {
            case ADMIN -> {
                hospitalId = null; laboratoryId = null; scopeType = "GLOBAL"; scopeId = 0;
            }
            case DOCTOR, HOSPITAL_ADMIN -> {
                if (hospitalId == null) throw new BusinessException("医院和医生管理员角色必须归属医院");
                scopeType = "HOSPITAL"; scopeId = hospitalId;
            }
            case LAB_STAFF -> {
                if (laboratoryId == null) throw new BusinessException("实验人员角色必须归属实验室");
                scopeType = "LABORATORY"; scopeId = laboratoryId;
            }
            case COURIER -> {
                hospitalId = null; laboratoryId = null; scopeType = "COURIER"; scopeId = userId;
            }
            default -> throw new BusinessException("角色无效");
        }
        jdbc.update("INSERT INTO lin_user_role(user_id,role_code,hospital_id,laboratory_id,scope_type,scope_id,created_by) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE hospital_id=VALUES(hospital_id),laboratory_id=VALUES(laboratory_id),is_active=1",
                userId, role.name(), hospitalId, laboratoryId, scopeType, scopeId, operatorId);
    }

    @Transactional
    public void remove(long assignmentId, int userId) {
        if (jdbc.update("UPDATE lin_user_role SET is_active=0 WHERE id=? AND user_id=? AND is_active=1", assignmentId, userId) != 1) {
            throw new BusinessException(404, "角色分配不存在");
        }
    }

    private PlatformRole role(String value) {
        try { return PlatformRole.valueOf(value); }
        catch (Exception error) { throw new BusinessException("角色无效"); }
    }

    private UserRoleAssignment assignment(Map<String, Object> row) {
        return new UserRoleAssignment(number(row.get("id")).longValue(), number(row.get("user_id")).intValue(),
                (String) row.get("role_code"), integer(row.get("hospital_id")), integer(row.get("laboratory_id")),
                (String) row.get("scope_type"), integer(row.get("scope_id")));
    }

    private Integer integer(Object value) { return value == null ? null : number(value).intValue(); }
    private Number number(Object value) { return (Number) value; }
}
