package com.petlis.common.security;

import com.petlis.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class ReviewPermission {
    @Value("${app.review.admin-user-ids:}")
    private String adminUserIds;
    public boolean isReviewer() {
        if (!SecurityUtils.isAuthenticated() || adminUserIds == null) return false;
        return isReviewerId(SecurityUtils.getCurrentUserId());
    }
    public boolean isReviewerId(Integer id) {
        if (id == null || adminUserIds == null) return false;
        String userId = String.valueOf(id);
        return Arrays.stream(adminUserIds.split(",")).map(String::trim).anyMatch(userId::equals);
    }
    public void requireReviewer() {
        if (!isReviewer()) throw new BusinessException(403, "需要审核人员权限");
    }
}
