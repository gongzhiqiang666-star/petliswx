package com.petlis.controller.api;

import com.petlis.service.impl.AdminCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminCatalogControllerTest {
    @Test void itemCrudUsesLegacySuccessContract() throws Exception {
        AdminCatalogService service = mock(AdminCatalogService.class);
        when(service.createItem(eq(2), any())).thenReturn(91L);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminCatalogController(service)).build();
        String body = "{\"name\":\"血常规\",\"code\":\"CBC\",\"price\":88.50}";
        mvc.perform(post("/admin/catalog/laboratories/2/items").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").value(91));
        mvc.perform(put("/admin/catalog/laboratories/2/items/91").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/admin/catalog/laboratories/2/items/91"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).updateItem(eq(2), eq(91), any());
        verify(service).deleteItem(2, 91);
    }
}
