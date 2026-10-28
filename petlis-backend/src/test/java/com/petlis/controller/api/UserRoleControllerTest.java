package com.petlis.controller.api;

import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserRoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserRoleControllerTest {
    @Autowired MockMvc mvc;
    @MockBean AccessService access;
    @MockBean com.petlis.common.util.JwtUtil jwt;
    @MockBean com.petlis.service.impl.SessionService sessions;

    @Test
    void currentRolesUseLegacySuccessEnvelope() throws Exception {
        when(access.current()).thenReturn(new CurrentActor(7, Set.of(PlatformRole.HOSPITAL_ADMIN), Set.of(1), Set.of(),
                new DataScope(false, Set.of(1), Set.of(), null)));
        mvc.perform(get("/user/roles")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data[0]").value("HOSPITAL_ADMIN"));
    }
}
