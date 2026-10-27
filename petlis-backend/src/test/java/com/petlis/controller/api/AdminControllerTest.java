package com.petlis.controller.api;

import com.petlis.common.security.PlatformRole;
import com.petlis.common.util.JwtUtil;
import com.petlis.config.SecurityConfig;
import com.petlis.service.impl.AdminService;
import com.petlis.service.impl.ReviewService;
import com.petlis.service.impl.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminController.class, ReviewController.class})
@Import(SecurityConfig.class)
class AdminControllerTest {
    @Autowired MockMvc mvc;
    @MockBean JwtUtil jwt;
    @MockBean SessionService sessions;
    @MockBean AdminService admin;
    @MockBean ReviewService reviews;

    @BeforeEach
    void token() {
        when(sessions.valid("admin_token")).thenReturn(true);
        when(sessions.valid("doctor_token")).thenReturn(true);
        when(jwt.getUserIdFromToken(anyString())).thenReturn(7);
        when(jwt.getHospitalIdFromToken(anyString())).thenReturn(null);
        when(jwt.getCityIdFromToken(anyString())).thenReturn(null);
        when(sessions.roles(7)).thenReturn(Set.of(PlatformRole.ADMIN));
    }

    @Test
    void adminUsersUseCode200AndZeroBasedPagination() throws Exception {
        when(admin.users("ACTIVE", null, null, 0, 20)).thenReturn(Map.of(
                "items", List.of(Map.of("id", 8, "roles", List.of("DOCTOR"))), "total", 1, "page", 0, "size", 20));

        mvc.perform(get("/admin/users").param("status", "ACTIVE").param("page", "0")
                        .header("Authorization", "Bearer admin_token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(0)).andExpect(jsonPath("$.data.items[0].id").value(8));
    }

    @Test
    void mappedDoctorCannotCallAdminOrReviewerApis() throws Exception {
        when(sessions.roles(7)).thenReturn(Set.of(PlatformRole.DOCTOR));

        mvc.perform(get("/admin/users").header("Authorization", "Bearer doctor_token"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/review/registrations").header("Authorization", "Bearer doctor_token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mappedAdminCanUseExistingReviewerApi() throws Exception {
        when(reviews.registrations(0, 20)).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));

        mvc.perform(get("/review/registrations").header("Authorization", "Bearer admin_token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void legacyReviewerTokenRemainsCompatible() throws Exception {
        when(jwt.isReviewerToken("admin_token")).thenReturn(true);
        when(sessions.roles(7)).thenReturn(Set.of());
        when(reviews.invoices(0, 20)).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));

        mvc.perform(get("/review/invoices").header("Authorization", "Bearer admin_token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void roleAssignmentUsesAuthenticatedOperatorNotClientSuppliedUserId() throws Exception {
        mvc.perform(post("/admin/users/8/roles").header("Authorization", "Bearer admin_token")
                        .contentType("application/json").content("{\"roleCode\":\"HOSPITAL_ADMIN\",\"hospitalId\":12}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        verify(admin).assignRole(eq(8), eq("HOSPITAL_ADMIN"), eq(12), isNull());
    }
}
