package com.petlis.common.security;

import com.petlis.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class ReviewPermission {
    @Value("${app.review.admin-user-ids:}")
    private String adminUserIds;
    private final UserRoleService roles;

    public ReviewPermission() { this.roles = null; }
    @Autowired public ReviewPermission(UserRoleService roles) { this.roles = roles; }

    public boolean isReviewer() {
        return SecurityUtils.isAuthenticated() && isReviewerId(SecurityUtils.getCurrentUserId());
    }
    public boolean isReviewerId(Integer id) {
        return isLegacyReviewerId(id) || id != null && roles != null && roles.roles(id).contains(PlatformRole.ADMIN);
    }
    public boolean isLegacyReviewerId(Integer id) {
        if (id == null || adminUserIds == null) return false;
        String userId = String.valueOf(id);
        return Arrays.stream(adminUserIds.split(",")).map(String::trim).anyMatch(userId::equals);
    }
    public void requireReviewer() {
        if (!isReviewer()) throw new BusinessException(403, "需要审核人员权限");
    }
}
