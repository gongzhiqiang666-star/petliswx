package com.petlis.contract;

import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.common.util.JwtUtil;
import com.petlis.config.SecurityConfig;
import com.petlis.service.impl.SessionService;
import com.petlis.controller.api.AuthController;
import com.petlis.controller.api.OrderController;
import com.petlis.controller.api.PetController;
import com.petlis.controller.api.ReportController;
import com.petlis.dto.request.OrderQueryRequest;
import com.petlis.dto.request.ReportQueryRequest;
import com.petlis.dto.response.OrderDetailResponse;
import com.petlis.dto.response.OrderResponse;
import com.petlis.dto.response.ReportResponse;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.service.AuthService;
import com.petlis.service.OrderService;
import com.petlis.service.PetService;
import com.petlis.service.ReportService;
import com.petlis.vo.PetVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, PetController.class, OrderController.class, ReportController.class})
@Import(SecurityConfig.class)
class LegacyApiContractTest {
    @Autowired MockMvc mvc;
    @MockBean JwtUtil jwt;
    @MockBean SessionService sessions;
    @MockBean AuthService auth;
    @MockBean PetService pets;
    @MockBean OrderService orders;
    @MockBean ReportService reports;
    @MockBean InfoHospitalRepository hospitals;

    @org.junit.jupiter.api.BeforeEach
    void authenticatedFixture() {
        when(sessions.valid("test_token_123")).thenReturn(true);
        when(jwt.getUserIdFromToken("test_token_123")).thenReturn(7);
        when(jwt.getHospitalIdFromToken("test_token_123")).thenReturn(1);
        when(jwt.getCityIdFromToken("test_token_123")).thenReturn(1);
    }

    @Test
    void loginKeepsLegacySuccessEnvelopeAndIdentifiers() throws Exception {
        when(auth.wechatLogin(any())).thenReturn(WechatLoginResponse.builder()
                .token("session").userId(7).hospitalId(2).build());

        mvc.perform(post("/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"mock-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.hospitalId").value(2));
    }

    @Test
    void petAndOrderPagesStayZeroBasedSpringPages() throws Exception {
        when(pets.query(any(), any())).thenReturn(new PageImpl<>(List.of(new PetVO()), PageRequest.of(0, 1), 1));
        when(orders.queryOrders(any())).thenReturn(new PageImpl<>(List.of(new OrderResponse()), PageRequest.of(0, 1), 1));

        mvc.perform(get("/pets?page=0&size=1").header("Authorization", "Bearer test_token_123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.content").isArray());
        mvc.perform(get("/orders?page=0&size=1").header("Authorization", "Bearer test_token_123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.number").value(0));

        var orderQuery = ArgumentCaptor.forClass(OrderQueryRequest.class);
        verify(orders).queryOrders(orderQuery.capture());
        assertEquals(0, orderQuery.getValue().getPage());
    }

    @Test
    void reportPageStaysZeroBasedAndKeepsLegacyDetailId() throws Exception {
        var report = new ReportResponse();
        report.setReportId(19);
        when(reports.queryReports(any())).thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 1), 1));

        mvc.perform(get("/reports?pageNum=0&pageSize=1").header("Authorization", "Bearer test_token_123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.content[0].reportId").value(19));

        var query = ArgumentCaptor.forClass(ReportQueryRequest.class);
        verify(reports).queryReports(query.capture());
        assertEquals(0, query.getValue().getPageNum());
    }

    @Test
    void orderDetailKeepsItemsPaginationShapeAndLegacyOrderId() throws Exception {
        var detail = new OrderDetailResponse();
        detail.setOrderId(23);
        var item = new OrderDetailResponse.OrderItemResponse();
        item.setItemId(31);
        detail.setItems(List.of(item));
        when(orders.getOrderDetail(23)).thenReturn(detail);

        mvc.perform(get("/orders/23").header("Authorization", "Bearer test_token_123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(23))
                .andExpect(jsonPath("$.data.items[0].itemId").value(31));
    }
}
