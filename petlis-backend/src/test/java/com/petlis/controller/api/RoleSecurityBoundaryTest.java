package com.petlis.controller.api;

import com.petlis.common.security.PlatformRole;
import com.petlis.common.util.JwtUtil;
import com.petlis.config.SecurityConfig;
import com.petlis.service.PetService;
import com.petlis.service.impl.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
@Import(SecurityConfig.class)
class RoleSecurityBoundaryTest {
    @Autowired MockMvc mvc;
    @MockBean JwtUtil jwt;
    @MockBean SessionService sessions;
    @MockBean PetService pets;

    @BeforeEach
    void authenticated() {
        when(sessions.valid("staff_token")).thenReturn(true);
        when(jwt.getUserIdFromToken("staff_token")).thenReturn(7);
        when(jwt.getHospitalIdFromToken("staff_token")).thenReturn(null);
        when(jwt.getCityIdFromToken("staff_token")).thenReturn(null);
        when(jwt.isStaffToken("staff_token")).thenReturn(true);
        when(pets.query(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    @Test
    void labStaffCannotUseLegacyDoctorPetApi() throws Exception {
        when(sessions.roles(7)).thenReturn(Set.of(PlatformRole.LAB_STAFF));
        mvc.perform(get("/pets").header("Authorization", "Bearer staff_token")).andExpect(status().isForbidden());
    }

    @Test
    void courierCannotUseLegacyDoctorPetApi() throws Exception {
        when(sessions.roles(7)).thenReturn(Set.of(PlatformRole.COURIER));
        mvc.perform(get("/pets").header("Authorization", "Bearer staff_token")).andExpect(status().isForbidden());
    }

    @Test
    void mappedDoctorKeepsLegacyDoctorPetApi() throws Exception {
        when(sessions.roles(7)).thenReturn(Set.of(PlatformRole.DOCTOR));
        mvc.perform(get("/pets").header("Authorization", "Bearer staff_token")).andExpect(status().isOk());
    }

    @Test
    void unmappedLegacyDoctorKeepsLegacyDoctorPetApi() throws Exception {
        when(sessions.roles(7)).thenReturn(Set.of());
        when(jwt.isStaffToken("staff_token")).thenReturn(false);
        mvc.perform(get("/pets").header("Authorization", "Bearer staff_token")).andExpect(status().isOk());
    }
}
