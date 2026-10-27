package com.petlis.controller.api;

import com.petlis.service.impl.ReportWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportWorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportWorkflowControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ReportWorkflowService reports;
    @MockBean com.petlis.common.util.JwtUtil jwt;
    @MockBean com.petlis.service.impl.SessionService sessions;

    @Test
    void reportWorkflowsUseLegacySuccessAndZeroBasedPages() throws Exception {
        when(reports.list(isNull(), eq(0), eq(20))).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        mvc.perform(get("/report-workflows")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.page").value(0));
    }
}
