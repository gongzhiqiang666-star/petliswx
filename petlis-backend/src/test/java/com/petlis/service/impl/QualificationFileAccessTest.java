package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QualificationFileAccessTest {
    @TempDir Path root;

    @Test
    void ordinaryDoctorFromSameHospitalCannotDownloadAnotherUsersQualification() {
        FeatureStore store = mock(FeatureStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        AccessService access = mock(AccessService.class);
        ReviewPermission reviewer = mock(ReviewPermission.class);
        when(access.current()).thenReturn(new CurrentActor(7, Set.of(PlatformRole.DOCTOR), Set.of(1), Set.of(),
                new DataScope(false, Set.of(1), Set.of(), null)));
        when(store.one(contains("lin_file"), eq(51L))).thenReturn(Map.of("id", 51L, "biz_type", "QUALIFICATION", "owner_user_id", 8, "hospital_id", 1));
        var service = new PlatformFileService(store, access, reviewer, root.toString(), 1024);
        assertThrows(BusinessException.class, () -> service.download(51));
        verify(access, never()).requireHospital(anyInt());
    }

    @Test
    void authorizedReviewerCanReadQualificationThroughSameStorageResolver() throws Exception {
        FeatureStore store = mock(FeatureStore.class);
        AccessService access = mock(AccessService.class);
        ReviewPermission reviewer = mock(ReviewPermission.class);
        when(reviewer.isReviewer()).thenReturn(true);
        when(access.current()).thenReturn(new CurrentActor(7, Set.of(PlatformRole.DOCTOR), Set.of(1), Set.of(),
                new DataScope(false, Set.of(1), Set.of(), null)));
        Files.writeString(root.resolve("qualification.pdf"), "%PDF-1");
        when(store.one(contains("lin_file"), eq(51L))).thenReturn(Map.of("id", 51L, "biz_type", "QUALIFICATION", "owner_user_id", 8,
                "hospital_id", 1, "storage_path", "qualification.pdf", "original_name", "资质.pdf", "mime_type", "application/pdf"));
        var service = new PlatformFileService(store, access, reviewer, root.toString(), 1024);
        assertEquals(200, service.download(51).getStatusCode().value());
    }
}
