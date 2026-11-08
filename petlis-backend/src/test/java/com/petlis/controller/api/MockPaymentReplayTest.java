package com.petlis.controller.api;
import com.petlis.service.PaymentService;
import com.petlis.service.impl.PaymentNotificationService;
import com.petlis.config.WeChatPayConfig;
import com.petlis.common.exception.BusinessException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MockPaymentReplayTest {
    PaymentService payments=mock(PaymentService.class);WeChatPayConfig config=mock(WeChatPayConfig.class);Environment env=mock(Environment.class);
    PaymentController controller=new PaymentController(payments,mock(PaymentNotificationService.class),config,env);
    @Test void concurrentPendingReplaysUseTheSameTransactionAndDifferentOrdersDoNot(){when(config.getMockEnabled()).thenReturn(true);when(payments.getPaymentStatus(anyString())).thenReturn(0);when(payments.handlePaymentCallback(anyString(),anyString())).thenReturn(true);
        controller.mockPaymentCallback("PAY_A",true);controller.mockPaymentCallback("PAY_A",true);controller.mockPaymentCallback("PAY_B",true);
        var ids=ArgumentCaptor.forClass(String.class);verify(payments,times(3)).handlePaymentCallback(anyString(),ids.capture());assertEquals(ids.getAllValues().get(0),ids.getAllValues().get(1));assertNotEquals(ids.getAllValues().get(0),ids.getAllValues().get(2));
    }
    @Test void legacyPaidMockReplayDoesNotReplaceItsExistingTransaction(){when(config.getMockEnabled()).thenReturn(true);when(payments.getPaymentStatus("OLD")).thenReturn(1);controller.mockPaymentCallback("OLD",true);verify(payments,never()).handlePaymentCallback(anyString(),anyString());}
    @Test void disabledOrProductionMockNeverReadsOrChangesPayment(){when(config.getMockEnabled()).thenReturn(false);assertThrows(BusinessException.class,()->controller.mockPaymentCallback("A",true));when(config.getMockEnabled()).thenReturn(true);when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true);assertThrows(BusinessException.class,()->controller.mockPaymentCallback("A",true));verifyNoInteractions(payments);}
    @Test void ownershipFailureNeverReachesCallback(){when(config.getMockEnabled()).thenReturn(true);when(payments.getPaymentStatus("OTHER")).thenThrow(new BusinessException(403,"无权访问"));assertThrows(BusinessException.class,()->controller.mockPaymentCallback("OTHER",true));verify(payments,never()).handlePaymentCallback(anyString(),anyString());}
}
