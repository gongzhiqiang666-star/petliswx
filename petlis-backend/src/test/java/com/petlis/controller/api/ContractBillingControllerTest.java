package com.petlis.controller.api;

import com.petlis.service.impl.ContractService;
import com.petlis.service.impl.MonthlyBillService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContractBillingControllerTest {
    @Test void contractAndBillEndpointsExposeLegacySuccessAndZeroBasedPages() throws Exception {
        ContractService contracts = mock(ContractService.class);
        MonthlyBillService bills = mock(MonthlyBillService.class);
        when(contracts.list(0, 20)).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        when(bills.list(null, null, false, 0, 20)).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        when(bills.get(21)).thenReturn(Map.of("id", 21));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ContractController(contracts), new MonthlyBillController(bills)).build();

        mvc.perform(get("/admin/contracts")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.page").value(0));
        mvc.perform(get("/bills")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/bills/21")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(21));
        mvc.perform(get("/admin/bills/21/items")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(patch("/admin/contracts/21/status").contentType("application/json").content("{\"active\":false,\"reason\":\"暂停\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(contracts).setStatus(21, false, "暂停");
    }
}
