package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class RechargePolicyTest {
    FeatureStore store;JdbcTemplate jdbc;WeChatPaymentGateway gateway;BusinessNotifications notifications;RechargeService service;
    @BeforeEach void setup(){store=mock(FeatureStore.class);jdbc=mock(JdbcTemplate.class);when(store.jdbc()).thenReturn(jdbc);gateway=mock(WeChatPaymentGateway.class);notifications=mock(BusinessNotifications.class);service=new RechargeService(store,gateway,notifications);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(7,null,10,1),null,List.of()));}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    Map<String,Object> order(String status){return new HashMap<>(Map.of("id",3,"user_id",7,"payment_no","RC-test","amount",new BigDecimal("20.00"),"status",status,"transaction_id","txn-original"));}
    @Test void duplicateVerifiedCallbackDoesNotCreditTwice(){when(store.one(contains("lin_recharge_order"),eq("RC-test"))).thenReturn(order("paid"));service.verifiedCallback("RC-test",2000,"txn-original");verify(jdbc,never()).update(anyString(),any(Object[].class));verifyNoInteractions(notifications);}
    @Test void duplicateCallbackCannotChangeTransaction(){when(store.one(contains("lin_recharge_order"),eq("RC-test"))).thenReturn(order("paid"));assertThrows(BusinessException.class,()->service.verifiedCallback("RC-test",2000,"txn-forged"));verifyNoInteractions(notifications);}
    @Test void wrongCallbackAmountCannotCredit(){when(store.one(contains("lin_recharge_order"),eq("RC-test"))).thenReturn(order("pending"));assertThrows(BusinessException.class,()->service.verifiedCallback("RC-test",1,"txn-original"));verify(jdbc,never()).update(anyString(),any(Object[].class));}
    @Test void closedRechargeCannotCredit(){when(store.one(contains("lin_recharge_order"),eq("RC-test"))).thenReturn(order("closed"));assertThrows(BusinessException.class,()->service.verifiedCallback("RC-test",2000,"txn-original"));verify(jdbc,never()).update(anyString(),any(Object[].class));}
    @Test void verifiedPaymentAddsExactBalanceAndLedger(){when(store.one(contains("lin_recharge_order"),eq("RC-test"))).thenReturn(order("pending"));when(store.one(contains("SELECT money"),eq(7))).thenReturn(Map.of("money",new BigDecimal("10.50")));service.verifiedCallback("RC-test",2000,"txn-original");verify(jdbc).update(contains("UPDATE lin_user SET money"),eq(new BigDecimal("30.50")),eq(7));verify(jdbc).update(contains("INSERT INTO lin_user_money_log"),eq(7),eq(new BigDecimal("20.00")),eq(new BigDecimal("10.50")),eq(new BigDecimal("30.50")),eq("个人充值 RC-test"));verify(notifications).emit(eq(7),eq("wallet"),eq("RC-test"),anyString(),anyString(),anyString());}
    @Test void replayCannotChangeRechargeAmount(){when(store.one(contains("SELECT money,openid"),eq(7))).thenReturn(Map.of("money",BigDecimal.ZERO));when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(order("pending")));assertThrows(BusinessException.class,()->service.create(new RechargeService.Request(new BigDecimal("30.00"),"recharge-test-123456")));verifyNoInteractions(gateway);}
    @Test void fractionalFenCannotBeRequested(){assertThrows(BusinessException.class,()->service.create(new RechargeService.Request(new BigDecimal("1.001"),"recharge-test-123456")));verifyNoInteractions(gateway);}
}
