package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.UserPrincipal;
import com.petlis.config.WeChatPayConfig;
import com.petlis.dto.request.PaymentCreateRequest;
import com.petlis.entity.business.*;
import com.petlis.repository.*;
import com.petlis.util.WeChatPayHttpClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServicePolicyTest {
    @Mock WeChatPaymentRepository payments;
    @Mock ListDiagnosisProjectRepository orders;
    @Mock UserRepository users;
    @Mock WeChatPayConfig config;
    @Mock WeChatPayHttpClient client;
    @Mock CouponService coupons;
    @Mock BusinessNotifications notifications;
    @InjectMocks PaymentServiceImpl service;
    private ListDiagnosisProject order;
    private User user;
    @BeforeEach void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(7,"server-openid",10,1),null,java.util.List.of()));
        user=new User();user.setId(7);user.setOpenid("server-openid");
        order=new ListDiagnosisProject();order.setListDiagnosisProjectId(1);order.setInfoHospitalId(10);order.setCreateSampleId(7);order.setSampleStatus(-3);order.setIsPayMoney(0);order.setTestFee(19.99);order.setAnimalName("unit-test-pet");
    }
    @AfterEach void cleanUp(){SecurityContextHolder.clearContext();}
    private PaymentCreateRequest request(){var r=new PaymentCreateRequest();r.setOrderId(1);r.setAmount(new BigDecimal("0.01"));r.setOpenid("forged-openid");return r;}
    private void stubOrder(){when(users.findById(7)).thenReturn(Optional.of(user));when(orders.findLockedById(1)).thenReturn(Optional.of(order));}
    @Test void rejectsOtherHospital(){stubOrder();order.setInfoHospitalId(11);assertThrows(BusinessException.class,()->service.createPayment(request()));verifyNoInteractions(payments);}
    @Test void rejectsOtherDoctor(){stubOrder();order.setCreateSampleId(8);assertThrows(BusinessException.class,()->service.createPayment(request()));verifyNoInteractions(payments);}
    @Test void rejectsPaymentBeforeReview(){stubOrder();order.setSampleStatus(0);assertThrows(BusinessException.class,()->service.createPayment(request()));verifyNoInteractions(payments);}
    @Test void ignoresClientAmountAndIdentity(){
        stubOrder();when(payments.findByOrderIdAndStatus(1,0)).thenReturn(Optional.empty());when(config.getMockEnabled()).thenReturn(true);
        AtomicReference<WeChatPayment> saved=new AtomicReference<>();when(payments.save(any())).thenAnswer(i->{saved.set(i.getArgument(0));return saved.get();});
        when(payments.findByPaymentNo(anyString())).thenAnswer(i->Optional.of(saved.get()));
        service.createPayment(request());
        assertEquals(new BigDecimal("19.99"),saved.get().getAmount());assertEquals(1999,saved.get().getTotalFee());assertEquals("server-openid",saved.get().getOpenid());
    }
    @Test void duplicateVerifiedCallbackIsIdempotent(){
        var payment=new WeChatPayment();payment.setStatus(1);payment.setTransactionId("verified-transaction");
        when(payments.findLockedByPaymentNo("PAY-test")).thenReturn(Optional.of(payment));
        assertTrue(service.handlePaymentCallback("PAY-test","verified-transaction"));verifyNoInteractions(orders);verify(payments,never()).save(any());
    }
    @Test void duplicateCallbackCannotChangeTransaction(){
        var payment=new WeChatPayment();payment.setStatus(1);payment.setTransactionId("verified-transaction");
        when(payments.findLockedByPaymentNo("PAY-test")).thenReturn(Optional.of(payment));
        assertThrows(BusinessException.class,()->service.handlePaymentCallback("PAY-test","different-transaction"));
    }
    @Test void cancelledPaymentCanResumeSameProviderOrder(){
        stubOrder();var p=new WeChatPayment();p.setPaymentNo("pending-payment");p.setOrderId(1);p.setUserId(7);p.setAmount(new BigDecimal("19.99"));p.setPrepayId("mock_existing");p.setCreateAt(new java.util.Date());when(payments.findByOrderIdAndStatus(1,0)).thenReturn(Optional.of(p));when(config.getMockEnabled()).thenReturn(true);
        var response=service.createPayment(request());assertEquals("pending-payment",response.getPaymentNo());assertTrue(response.isMock());verify(payments,never()).save(any());
    }

}
