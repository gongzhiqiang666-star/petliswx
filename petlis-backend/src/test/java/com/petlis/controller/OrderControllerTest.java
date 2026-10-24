package com.petlis.controller;

import com.petlis.dto.response.OrderDetailResponse;
import com.petlis.dto.response.OrderResponse;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.business.DiagnosisDetail;
import com.petlis.entity.dict.InfoTestType;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 订单控制器测试类
 *
 * 测试订单管理API及安全PDF URL生成
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private InfoHospitalRepository hospitalRepository;

    private OrderDetailResponse mockOrderDetail;
    private List<DiagnosisDetail> mockDetails;

    @BeforeEach
    void setUp() {
        // 准备模拟数据
        mockOrderDetail = new OrderDetailResponse();
        mockOrderDetail.setOrderId(1);
        mockOrderDetail.setUserNO("ORDER20240101001");
        mockOrderDetail.setSampleStatus(3); // 已完成
        mockOrderDetail.setTestFee(100.00);
        mockOrderDetail.setHospitalId(1);

        // 创建检测项
        List<OrderDetailResponse.OrderItemResponse> items = new ArrayList<>();

        // 已完成且有PDF的项目
        OrderDetailResponse.OrderItemResponse item1 = new OrderDetailResponse.OrderItemResponse();
        item1.setItemId(1);
        item1.setTestTypeName("血常规检查");
        item1.setTestPrice(new BigDecimal("50.00"));
        item1.setDiagnosisStatus(2); // 已完成
        item1.setIsSendMail(1); // PDF已生成
        item1.setPdfHttpUrl("/Content/FilePdf/report_1.pdf");
        item1.setPdfFullUrl("https://laboklin.cn/Content/FilePdf/report_1.pdf");
        items.add(item1);

        // 已完成但无PDF的项目
        OrderDetailResponse.OrderItemResponse item2 = new OrderDetailResponse.OrderItemResponse();
        item2.setItemId(2);
        item2.setTestTypeName("生化检查");
        item2.setTestPrice(new BigDecimal("50.00"));
        item2.setDiagnosisStatus(2);
        item2.setIsSendMail(0); // PDF未生成
        items.add(item2);

        mockOrderDetail.setItems(items);

        // 模拟医院数据
        InfoHospital mockHospital = new InfoHospital();
        mockHospital.setInfoHospitalId(1);
        mockHospital.setPayType(0); // 现结客户
        when(hospitalRepository.findById(1)).thenReturn(Optional.of(mockHospital));
    }

    /**
     * 测试获取订单详情（成功）
     */
    @Test
    void testGetOrderDetail_Success() throws Exception {
        // Given: 模拟服务返回订单详情
        when(orderService.getOrderDetail(1)).thenReturn(mockOrderDetail);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/orders/1")
                        .header("Authorization", "Bearer test_token_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.userNO").value("ORDER20240101001"))
                .andExpect(jsonPath("$.data.items[0].securePdfUrl").exists())
                .andExpect(jsonPath("$.data.items[0].securePdfUrl").value(
                        org.hamcrest.Matchers.containsString("/api/pdf/view/1")))
                .andExpect(jsonPath("$.data.items[0].securePdfUrl").value(
                        org.hamcrest.Matchers.containsString("token=test_token_123")))
                .andExpect(jsonPath("$.data.items[1].securePdfUrl").doesNotExist());
    }

    /**
     * 测试获取订单详情（订单不存在）
     */
    @Test
    void testGetOrderDetail_NotFound() throws Exception {
        // Given: 模拟服务抛出异常
        when(orderService.getOrderDetail(999))
                .thenThrow(new IllegalArgumentException("订单不存在"));

        // When & Then: 应该返回错误
        mockMvc.perform(get("/orders/999")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 测试获取订单详情（无Authorization头）
     */
    @Test
    void testGetOrderDetail_NoAuthHeader() throws Exception {
        // Given: 模拟服务返回订单详情
        when(orderService.getOrderDetail(1)).thenReturn(mockOrderDetail);

        // When & Then: 执行请求（没有Authorization头）
        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // securePdfUrl应该存在但token为空
                .andExpect(jsonPath("$.data.items[0].securePdfUrl").value(
                        org.hamcrest.Matchers.containsString("token=")));
    }

    /**
     * 测试查询订单列表（成功）
     */
    @Test
    void testQueryOrders_Success() throws Exception {
        // Given: 准备订单列表数据
        List<OrderResponse> orders = new ArrayList<>();
        OrderResponse order = new OrderResponse();
        order.setOrderId(1);
        order.setUserNO("ORDER20240101001");
        order.setAnimalName("测试宠物");
        order.setSampleStatus(3);
        orders.add(order);

        org.springframework.data.domain.Page<OrderResponse> page =
                new org.springframework.data.domain.PageImpl<>(orders);

        when(orderService.queryOrders(any())).thenReturn(page);

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].orderId").value(1))
                .andExpect(jsonPath("$.data.content[0].animalName").value("测试宠物"));
    }

    /**
     * 测试计算订单总费用（成功）
     */
    @Test
    void testCalculateTotalFee_Success() throws Exception {
        // Given: 模拟服务返回总费用
        when(orderService.calculateTotalFee(1)).thenReturn(new BigDecimal("100.00"));

        // When & Then: 执行请求并验证结果
        mockMvc.perform(get("/orders/1/fee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(100.00));
    }

    /**
     * 测试创建订单（现结客户）
     */
    @Test
    void testCreateOrder_NonMonthlyCustomer() throws Exception {
        // Given: 模拟服务创建订单
        when(orderService.createOrder(any())).thenReturn(1);
        when(orderService.getOrderDetail(1)).thenReturn(mockOrderDetail);

        String requestBody = """
                {
                    "hospitalId": 1,
                    "animalName": "测试宠物",
                    "animalType": "金毛",
                    "items": [
                        {"testTypeId": 1, "cityId": 1}
                    ]
                }
                """;

        // When & Then: 执行请求并验证结果
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.needPayment").value(true))
                .andExpect(jsonPath("$.data.payTypeDesc").value("现结客户"));
    }

    /**
     * 测试创建订单（月结客户）
     */
    @Test
    void testCreateOrder_MonthlyCustomer() throws Exception {
        // Given: 模拟月结客户医院
        InfoHospital monthlyHospital = new InfoHospital();
        monthlyHospital.setInfoHospitalId(2);
        monthlyHospital.setPayType(1); // 月结
        when(hospitalRepository.findById(2)).thenReturn(Optional.of(monthlyHospital));

        when(orderService.createOrder(any())).thenReturn(2);

        OrderDetailResponse monthlyOrderDetail = new OrderDetailResponse();
        monthlyOrderDetail.setOrderId(2);
        monthlyOrderDetail.setHospitalId(2);
        when(orderService.getOrderDetail(2)).thenReturn(monthlyOrderDetail);

        String requestBody = """
                {
                    "hospitalId": 2,
                    "animalName": "测试宠物",
                    "items": [
                        {"testTypeId": 1, "cityId": 1}
                    ]
                }
                """;

        // When & Then: 执行请求并验证结果
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.needPayment").value(false))
                .andExpect(jsonPath("$.data.payTypeDesc").value("月结客户"));
    }

    /**
     * 测试更新订单状态（成功）
     */
    @Test
    void testUpdateOrderStatus_Success() throws Exception {
        // Given: 模拟服务更新成功
        org.mockito.Mockito.doNothing().when(orderService).updateOrderStatus(eq(1), eq(3));

        // When & Then: 执行请求并验证结果
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/orders/1/status")
                        .param("status", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("状态更新成功"));
    }
}
