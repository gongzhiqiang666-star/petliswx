package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import com.petlis.dto.response.PaymentResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Explicit opt-in integration tests against the isolated local copy; each fixture transaction rolls back. */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class RechargeLocalDatabaseTest {
    @Autowired RechargeService recharges;
    @Autowired FeatureStore store;
    @MockBean WeChatPaymentGateway gateway;
    int userId;
    @BeforeEach void fixture(){
        assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"),"Only isolated local database is allowed");
        String username="it_recharge_"+UUID.randomUUID().toString().replace("-", "").substring(0,12);
        userId=(int)store.insert("INSERT INTO lin_user(group_id,username,nickname,password,salt,money,score,level,gender,successions,maxsuccessions,loginfailure,status,createtime,updatetime,jointime) VALUES(0,?,'充值回归测试','','',10.50,0,0,0,0,0,0,'normal',UNIX_TIMESTAMP(),UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",username);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(userId,"fixture-openid",1,1),null,List.of()));
        when(gateway.create(anyString(),any(),nullable(String.class),anyString())).thenReturn("mock_fixture");
        when(gateway.parameters(anyString(),any(),anyString(),anyBoolean())).thenAnswer(call->{var response=new PaymentResponse();response.setPaymentNo(call.getArgument(0));response.setAmount(call.<BigDecimal>getArgument(1).toPlainString());response.setMock(true);return response;});
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    String create(){return recharges.create(new RechargeService.Request(new BigDecimal("20.00"),"recharge_fixture_key_123")).getPaymentNo();}
    BigDecimal balance(){return store.jdbc().queryForObject("SELECT money FROM lin_user WHERE id=?",BigDecimal.class,userId);}
    @Test void createDoesNotCreditBeforeVerifiedCallback(){create();assertEquals(new BigDecimal("10.50"),balance());assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_user_money_log WHERE user_id=?",Integer.class,userId));}
    @Test void verifiedCallbackAndReplayProduceOneLedgerEntry(){String payment=create(),transaction="fixture-"+UUID.randomUUID();recharges.verifiedCallback(payment,2000,transaction);recharges.verifiedCallback(payment,2000,transaction);assertEquals(new BigDecimal("30.50"),balance());assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_user_money_log WHERE user_id=?",Integer.class,userId));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_notification_event WHERE user_id=? AND event_type='wallet'",Integer.class,userId));}
    @Test void mismatchedAmountCannotCredit(){String payment=create();assertThrows(BusinessException.class,()->recharges.verifiedCallback(payment,1,"fixture-"+UUID.randomUUID()));assertEquals(new BigDecimal("10.50"),balance());}
    @Test void requestReplayDoesNotCreateAnotherProviderOrder(){String payment=create();assertEquals(payment,create());verify(gateway,times(1)).create(anyString(),any(),nullable(String.class),anyString());assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_recharge_order WHERE user_id=?",Integer.class,userId));}
    @Test void closedRechargeCannotBeCredited(){String payment=create();recharges.close(payment);assertThrows(BusinessException.class,()->recharges.verifiedCallback(payment,2000,"fixture-"+UUID.randomUUID()));assertEquals(new BigDecimal("10.50"),balance());}
}
