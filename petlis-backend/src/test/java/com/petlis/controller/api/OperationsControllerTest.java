package com.petlis.controller.api;

import com.petlis.service.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OperationsControllerTest {
    @Test void refundAndPointsAdjustmentUseRealAdminEndpoints() throws Exception {
        RefundService refunds = mock(RefundService.class);
        OperationalService operations = mock(OperationalService.class);
        when(refunds.refund(8, "标本不合格", "refund-key-1")).thenReturn(Map.of("orderId", 8, "status", "REFUNDED", "mock", true));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RefundController(refunds), new OperationalController(operations)).build();

        mvc.perform(post("/admin/refunds/8").contentType("application/json").content("{\"reason\":\"标本不合格\",\"requestKey\":\"refund-key-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.mock").value(true));
        mvc.perform(post("/admin/operations/points-adjustments").contentType("application/json").content("{\"userId\":5,\"delta\":20,\"reason\":\"补偿\",\"requestKey\":\"points-key-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(operations).adjustPoints(5, 20, "补偿", "points-key-1");
    }
}
