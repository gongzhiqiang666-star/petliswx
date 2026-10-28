package com.petlis.controller.api;

import com.petlis.service.PetService;
import com.petlis.service.impl.PetHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PetHealthControllerTest {
    @Autowired MockMvc mvc;
    @MockBean PetService pets;
    @MockBean PetHealthService health;
    @MockBean com.petlis.common.util.JwtUtil jwt;
    @MockBean com.petlis.service.impl.SessionService sessions;

    @Test
    void petHealthUsesLegacySuccessAndZeroBasedReportPage() throws Exception {
        when(health.health(7, 0, 20)).thenReturn(Map.of("petId", 7, "medicalHistory", List.of(),
                "reports", Map.of("items", List.of(), "total", 0, "page", 0, "size", 20)));
        mvc.perform(get("/pets/7/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.reports.page").value(0));
    }
}
