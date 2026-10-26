package com.petlis.service.impl;
import com.petlis.config.WeChatPayConfig;
import com.petlis.common.exception.BusinessException;
import com.petlis.entity.business.WeChatPayment;
import com.petlis.repository.WeChatPaymentRepository;
import com.petlis.service.PaymentService;
import com.petlis.util.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class PaymentNotificationTest {
 WeChatPayConfig config;WeChatPayHttpClient client;WeChatPaymentRepository repository;PaymentService payments;PaymentNotificationService service;
 @BeforeEach void setup(){config=new WeChatPayConfig();config.setApiKey("unit-test-only-not-a-real-key-123");config.setAppId("unit-app");config.setMchId("unit-merchant");client=new WeChatPayHttpClient();repository=mock(WeChatPaymentRepository.class);payments=mock(PaymentService.class);service=new PaymentNotificationService(config,client,repository,payments,mock(RechargeService.class));}
 Map<String,String> payload(){var m=new HashMap<String,String>();m.put("return_code","SUCCESS");m.put("result_code","SUCCESS");m.put("appid","unit-app");m.put("mch_id","unit-merchant");m.put("total_fee","1999");m.put("out_trade_no","unit-order");m.put("transaction_id","unit-transaction");return m;}
 String signed(Map<String,String> values){values.put("sign",WeChatPayUtil.generateMD5Sign(values,config.getApiKey()));return client.mapToXml(values);}
 @Test void validCallbackUsesVerifiedPayment(){var p=new WeChatPayment();p.setTotalFee(1999);when(repository.findByPaymentNo("unit-order")).thenReturn(Optional.of(p));service.handle(signed(payload()));verify(payments).handlePaymentCallback("unit-order","unit-transaction");}
 @Test void wrongAmountNeverMarksPaid(){var p=new WeChatPayment();p.setTotalFee(2000);when(repository.findByPaymentNo("unit-order")).thenReturn(Optional.of(p));assertThrows(BusinessException.class,()->service.handle(signed(payload())));verifyNoInteractions(payments);}
 @Test void forgedSignatureNeverReadsPayment(){var m=payload();m.put("sign","forged");assertThrows(BusinessException.class,()->service.handle(client.mapToXml(m)));verifyNoInteractions(repository,payments);}
 @Test void otherMerchantNeverReadsPayment(){var m=payload();m.put("mch_id","other-merchant");assertThrows(BusinessException.class,()->service.handle(signed(m)));verifyNoInteractions(repository,payments);}
 @Test void failedProviderResultNeverMarksPaid(){var m=payload();m.put("result_code","FAIL");assertThrows(BusinessException.class,()->service.handle(signed(m)));verifyNoInteractions(repository,payments);}
 @Test void missingTransactionNeverMarksPaid(){var m=payload();m.remove("transaction_id");var p=new WeChatPayment();p.setTotalFee(1999);when(repository.findByPaymentNo("unit-order")).thenReturn(Optional.of(p));assertThrows(BusinessException.class,()->service.handle(signed(m)));verifyNoInteractions(payments);}
}
