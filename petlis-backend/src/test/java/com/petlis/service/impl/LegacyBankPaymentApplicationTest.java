package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyBankPaymentApplicationTest {
 @Test void missingMerchantOrDisabledPrepareGateCannotAdvertiseOrCreatePayment(){
  var access=mock(AccessService.class);var jdbc=mock(JdbcTemplate.class);var store=new FeatureStore(jdbc,new ObjectMapper());
  var quotes=mock(LegacyBankPaymentQuoteService.class);when(quotes.capabilities()).thenReturn(Map.of("bankPayments",true,"bankPaymentCreate",false));
  when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of("config_value","{\"enabled\":true,\"prepareEnabled\":true}")));
  var preparation=mock(LegacyBankPaymentPreparationService.class);
  var app=new LegacyBankPaymentApplicationService(store,access,quotes,preparation,"");
  assertFalse(app.capabilities().get("bankPaymentCreate"));
  assertEquals(409,assertThrows(BusinessException.class,()->app.prepare(null)).getCode());verifyNoInteractions(preparation);
  when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of("config_value","{\"enabled\":true,\"prepareEnabled\":\"true\"}")));
  var enabled=new LegacyBankPaymentApplicationService(store,access,quotes,preparation,"LOCALTEST");assertFalse(enabled.capabilities().get("bankPaymentCreate"));
 }
 @Test void approvedPreparationUsesServerMerchantAndLeavesClinicalAndNetworkToOtherServices(){
  var access=mock(AccessService.class);var jdbc=mock(JdbcTemplate.class);var store=new FeatureStore(jdbc,new ObjectMapper());
  var quotes=mock(LegacyBankPaymentQuoteService.class);when(quotes.capabilities()).thenReturn(Map.of("bankPayments",true,"bankPaymentCreate",false));
  when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of("config_value","{\"enabled\":true,\"prepareEnabled\":true}")));
  var preparation=mock(LegacyBankPaymentPreparationService.class);var command=new LegacyBankPaymentPreparationService.Command("ORDER",7L,null,"O1","12.50","req1");
  var expected=new LegacyBankPaymentPreparationService.Prepared("LOCALTEST123","12.50","1250",1,"READY");when(preparation.prepare(command,"LOCALTEST")).thenReturn(expected);
  var app=new LegacyBankPaymentApplicationService(store,access,quotes,preparation,"LOCALTEST");assertTrue(app.capabilities().get("bankPaymentCreate"));assertEquals(expected,app.prepare(command));verify(preparation).prepare(command,"LOCALTEST");
 }
}
