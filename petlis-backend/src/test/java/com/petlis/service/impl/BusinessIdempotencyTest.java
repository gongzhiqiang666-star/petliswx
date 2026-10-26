package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.*;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class BusinessIdempotencyTest {
 FeatureStore store;JdbcTemplate jdbc;ObjectMapper json;ReviewPermission permission;BusinessNotifications notifications;
 @BeforeEach void setup(){jdbc=mock(JdbcTemplate.class);store=mock(FeatureStore.class);when(store.jdbc()).thenReturn(jdbc);json=new ObjectMapper();permission=mock(ReviewPermission.class);notifications=mock(BusinessNotifications.class);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(7,null,10,1),null,List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 InvoiceService invoice(String snapshot){when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("id",3L,"profile_snapshot",snapshot)));return new InvoiceService(notifications,store,permission,json);}
 @Test void invoiceReplayReturnsSameApplicationForSameUnorderedOrders(){var service=invoice("{\"profileId\":2,\"orderIds\":[4,5]}");assertEquals(3,service.apply(new InvoiceService.Application(2,List.of(5,4),"idempotency-key-123")));verify(store,never()).insert(anyString(),any(Object[].class));}
 @Test void invoiceReplayRejectsDifferentOrders(){var service=invoice("{\"profileId\":2,\"orderIds\":[4,5]}");assertThrows(BusinessException.class,()->service.apply(new InvoiceService.Application(2,List.of(4,6),"idempotency-key-123")));}
 @Test void invoiceReplayRejectsDifferentProfile(){var service=invoice("{\"profileId\":2,\"orderIds\":[4,5]}");assertThrows(BusinessException.class,()->service.apply(new InvoiceService.Application(3,List.of(4,5),"idempotency-key-123")));}
 @Test void redemptionReplayRejectsChangedAddress()throws Exception{var old=new MallService.Address("Test","19900000000","old address");when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("id",1,"gift_id",3,"quantity",1,"delivery_address",json.writeValueAsString(old))));var service=new MallService(notifications,store,permission,json);assertThrows(BusinessException.class,()->service.redeem(new MallService.Redemption(3,1,new MallService.Address("Test","19900000000","new address"),"idempotency-key-123")));verify(store,never()).insert(anyString(),any(Object[].class));}
}
