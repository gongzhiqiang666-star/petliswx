package com.petlis.controller;
import com.petlis.controller.api.OrderController;
import com.petlis.config.SecurityConfig;
import com.petlis.common.exception.BusinessException;
import com.petlis.dto.response.OrderDetailResponse;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
@WebMvcTest(OrderController.class) @Import(SecurityConfig.class)
class OrderControllerTest extends ControllerSecurityTestSupport {
 @Autowired MockMvc mvc;@MockBean OrderService orders;@MockBean InfoHospitalRepository hospitals;
 String payload="""
 {"infoAnimalId":1,"infoSampleTypeId":1,"animalName":"测试宠物","animalNameEn":"Test Pet","animalTypeId":1,"animalType":"犬","animalTypeEn":"Dog","age":"3Y","dictSexId":1,"sampleArea":"blood","masterName":"测试畜主","masterNameEn":"Test Owner","diagnosisDetails":[{"infoTestTypeId":1}]}
 """;
 @Test void anonymousCannotReadOrders()throws Exception{mvc.perform(get("/orders/1")).andExpect(status().isUnauthorized());verifyNoInteractions(orders);}
 @Test void authenticatedOrderDetailReturnsData()throws Exception{var detail=new OrderDetailResponse();detail.setOrderId(1);when(orders.getOrderDetail(1)).thenReturn(detail);mvc.perform(get("/orders/1").header("Authorization","Bearer test_token_123")).andExpect(status().isOk()).andExpect(jsonPath("$.data.orderId").value(1));}
 @Test void cashSubmissionWaitsForReviewWithoutPayment()throws Exception{submission(false);}
 @Test void monthlySubmissionWaitsForReviewWithoutPayment()throws Exception{submission(true);}
 void submission(boolean monthly)throws Exception{var h=new InfoHospital();h.setPayType(monthly?1:0);when(hospitals.findById(1)).thenReturn(Optional.of(h));var d=new OrderDetailResponse();d.setOrderId(1);d.setHospitalId(1);d.setTestFee(50d);when(orders.createOrder(any())).thenReturn(1);when(orders.getOrderDetail(1)).thenReturn(d);mvc.perform(post("/orders").header("Authorization","Bearer test_token_123").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.data.needPayment").value(false)).andExpect(jsonPath("$.data.payTypeDesc").value(monthly?"月结客户":"现结客户"));}
 @Test void invalidSubmissionIsRejectedBeforeService()throws Exception{mvc.perform(post("/orders").header("Authorization","Bearer test_token_123").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest());verifyNoInteractions(orders);}
 @Test void internalSqlDetailsAreNotReturnedToClient()throws Exception{when(orders.getOrderDetail(1)).thenThrow(new IllegalStateException("private database SQL"));mvc.perform(get("/orders/1").header("Authorization","Bearer test_token_123")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("操作失败，请稍后重试"));}
}
