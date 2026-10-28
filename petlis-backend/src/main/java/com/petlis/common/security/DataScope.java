package com.petlis.common.security;

import java.util.Set;

public record DataScope(
        boolean global,
        Set<Integer> hospitalIds,
        Set<Integer> laboratoryIds,
        Integer courierUserId
) {
    public DataScope {
        hospitalIds = Set.copyOf(hospitalIds);
        laboratoryIds = Set.copyOf(laboratoryIds);
    }
}
