package com.petlis.service.impl;

import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final FeatureStore store;
    private final AccessService access;

    public void record(String action, String resourceType, Object resourceId, Object before, Object after, String reason) {
        CurrentActor actor = access.current();
        String role = actor.roles().stream().map(Enum::name).sorted().findFirst().orElse("UNKNOWN");
        store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,actor_role,action,resource_type,resource_id,before_json,after_json,reason) VALUES(?,?,?,?,?,?,?,?)",
                actor.userId(), role, action, resourceType, String.valueOf(resourceId),
                before == null ? null : store.encode(before), after == null ? null : store.encode(after), reason);
    }
}
