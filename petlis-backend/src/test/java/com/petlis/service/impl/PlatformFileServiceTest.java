package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlatformFileServiceTest {
    @TempDir Path root;

    @Test
    void reportFileUsesRandomSafeNameAndPersistsOnlyMetadata() {
        FeatureStore store = mock(FeatureStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        when(store.insert(contains("lin_file"), any(Object[].class))).thenReturn(51L);
        var service = new PlatformFileService(store, mock(AccessService.class), root.toString(), 1024);
        var stored = service.saveReport(1, 2, 41, new byte[]{'%', 'P', 'D', 'F', '-', '1'});
        assertEquals(51L, stored.id());
        assertTrue(stored.url().startsWith("/Content/FilePdf/"));
        assertTrue(stored.url().endsWith(".pdf"));
        assertFalse(stored.url().contains(".."));
        verify(store).insert(contains("lin_file"), any(Object[].class));
    }

    @Test
    void magicAndSizeAreMandatory() {
        var service = new PlatformFileService(mock(FeatureStore.class), mock(AccessService.class), root.toString(), 4);
        assertThrows(BusinessException.class, () -> service.saveReport(1, 2, 41, new byte[]{'%', 'P', 'D', 'F', '-'}));
        assertThrows(BusinessException.class, () -> service.saveReport(1, 2, 41, new byte[]{1, 2, 3}));
    }

    @Test
    void downloadRejectsSymlinkEscapingStorageRoot() throws Exception {
        FeatureStore store = mock(FeatureStore.class);
        AccessService access = mock(AccessService.class);
        var actor = new com.petlis.common.security.CurrentActor(7, java.util.Set.of(com.petlis.common.security.PlatformRole.DOCTOR), java.util.Set.of(1), java.util.Set.of(),
                new com.petlis.common.security.DataScope(false, java.util.Set.of(1), java.util.Set.of(), null));
        when(access.current()).thenReturn(actor);
        when(store.one(contains("lin_file"), eq(51L))).thenReturn(java.util.Map.of("id", 51L, "biz_type", "GENERAL", "owner_user_id", 7,
                "storage_path", "linked.pdf", "original_name", "linked.pdf", "mime_type", "application/pdf"));
        var outside = root.resolveSibling("outside.pdf");
        java.nio.file.Files.writeString(outside, "secret");
        java.nio.file.Files.createSymbolicLink(root.resolve("linked.pdf"), outside);
        var service = new PlatformFileService(store, access, root.toString(), 1024);
        assertThrows(BusinessException.class, () -> service.download(51));
    }
}
