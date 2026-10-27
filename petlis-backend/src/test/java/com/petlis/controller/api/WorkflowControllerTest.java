package com.petlis.controller.api;

import com.petlis.service.impl.LabWorkflowService;
import com.petlis.service.impl.ShipmentWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({LogisticsWorkflowController.class, LabWorkflowController.class})
@AutoConfigureMockMvc(addFilters = false)
class WorkflowControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ShipmentWorkflowService shipments;
    @MockBean LabWorkflowService laboratory;
    @MockBean com.petlis.common.util.JwtUtil jwt;
    @MockBean com.petlis.service.impl.SessionService sessions;

    @Test
    void logisticsListUsesLegacySuccessAndZeroBasedPage() throws Exception {
        when(shipments.list(isNull(), eq(0), eq(20))).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        mvc.perform(get("/logistics")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void laboratoryListUsesLegacySuccessAndZeroBasedPage() throws Exception {
        when(laboratory.list(isNull(), eq(0), eq(20))).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        mvc.perform(get("/lab/tasks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void scanReceiptEndpointPassesBarcodeAndReturnsLegacySuccess() throws Exception {
        when(shipments.receiveScanned(11, "55550-8")).thenReturn(Map.of("id", 11, "status", "RECEIVED"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/logistics/11/scan-receive")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"barcode\":\"55550-8\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }
}
