package com.petlis.common.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAuthorizationRuleTest {
    @Test
    void adminNamespaceUsesEndpointRolesInsteadOfAdminOnly() throws Exception {
        String config = Files.readString(Path.of("src/main/java/com/petlis/config/SecurityConfig.java"));
        int scoped = config.indexOf(".requestMatchers(\"/admin/orders/**\", \"/admin/bills/**\", \"/admin/refunds/**\", \"/admin/print/**\")");
        assertTrue(scoped > 0);
        assertTrue(config.substring(scoped, config.indexOf('\n', scoped)).contains("hasAnyRole(\"ADMIN\", \"HOSPITAL_ADMIN\", \"LAB_STAFF\")"));
        int catalog = config.indexOf(".requestMatchers(\"/admin/catalog/**\")");
        assertTrue(catalog > 0);
        assertTrue(config.substring(catalog, config.indexOf('\n', catalog)).contains("hasAnyRole(\"ADMIN\", \"LAB_STAFF\")"));
        int fallback = config.indexOf(".requestMatchers(\"/admin/**\")");
        assertTrue(fallback > catalog);
        assertTrue(config.substring(fallback, config.indexOf('\n', fallback)).contains("hasRole(\"ADMIN\")"));
    }
}
