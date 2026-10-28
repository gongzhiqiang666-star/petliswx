package com.petlis.controller.api;

import com.petlis.service.impl.DashboardService;
import com.petlis.service.impl.PrintService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardPrintControllerTest {
    @Test void dashboardCsvAndPrintEndpointsReturnRealContentTypes() throws Exception {
        DashboardService dashboard = mock(DashboardService.class);
        PrintService print = mock(PrintService.class);
        when(dashboard.dashboard("2026-01-01", "2026-02-01")).thenReturn(Map.of("orderCount", 1L, "revenue", new BigDecimal("10.00"), "cashRevenue", BigDecimal.TEN,
                "monthlyRevenue", BigDecimal.ZERO, "arrears", BigDecimal.ZERO, "hospitalRanking", List.of(), "projectRanking", List.of()));
        when(dashboard.exportOrders("2026-01-01", "2026-02-01")).thenReturn("\uFEFF订单ID\r\n8\r\n");
        when(print.order(8)).thenReturn("<html>送检单</html>");
        when(print.specimenLabel(8)).thenReturn("<html><svg>标签</svg></html>");
        when(print.report(81)).thenReturn("<html>报告</html>");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DashboardController(dashboard), new PrintController(print)).build();

        mvc.perform(get("/admin/dashboard").param("from", "2026-01-01").param("to", "2026-02-01")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/admin/export/orders.csv").param("from", "2026-01-01").param("to", "2026-02-01")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"));
        mvc.perform(get("/admin/print/orders/8")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html"));
        mvc.perform(get("/admin/print/specimens/8")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("svg")));
        mvc.perform(get("/admin/print/reports/81")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("报告")));
    }
}
