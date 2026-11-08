package com.petlis.service.impl;

import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CouponServiceContractTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    ContractService contracts;
    CouponService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); contracts = mock(ContractService.class);
        when(store.jdbc()).thenReturn(jdbc);
        service = new CouponService(store, mock(ReviewPermission.class), contracts);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(5, null, 7, 1), null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void quoteUsesHospitalContractPriceAndRetainsYuanUnits() {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("price", new BigDecimal("100.00"))));
        when(store.one(contains("lin_laboratory"), eq(2))).thenReturn(Map.of("id", 2));
        when(contracts.priceForOrder(7, 2, List.of(91))).thenReturn(new ContractService.Resolution(11, true, "MONTHLY", Map.of(91,
                new ContractService.ItemPrice(101, new BigDecimal("100.00"), new BigDecimal("85.00"), new BigDecimal("8.5000")))));

        CouponService.Quote quote = service.quote(2, List.of(91), null);

        assertEquals(new BigDecimal("85.00"), quote.prices().get(91));
        assertEquals(new BigDecimal("85.00"), quote.payableAmount());
        assertEquals(Boolean.TRUE, quote.monthly());
    }

    @Test void orderSnapshotUsesContractSettlementInsteadOfMutableHospitalFlag() {
        CouponService.Quote quote = new CouponService.Quote(2, Map.of(91, new BigDecimal("85.00")), new BigDecimal("85.00"),
                BigDecimal.ZERO, new BigDecimal("85.00"), null, 11L, true, Map.of(91, 101L));
        service.recordOrder(8, quote, false);
        verify(jdbc).update(contains("lin_order_extension"), eq(8), eq(5), eq(2), isNull(), eq(new BigDecimal("85.00")),
                eq(BigDecimal.ZERO), eq(new BigDecimal("85.00")), eq("monthly"));
        verify(jdbc).update(contains("lin_order_pricing_snapshot"), eq(8), eq(11L), eq(7), eq(2), eq("monthly"),
                eq(new BigDecimal("85.00")), eq(BigDecimal.ZERO), eq(new BigDecimal("85.00")), any());
    }
    @Test void couponConflictsAndInvalidRulesAreNotServerErrors() {
        when(contracts.priceForOrder(7,2,List.of(91))).thenReturn(new ContractService.Resolution(11,true,"MONTHLY",Map.of(91,new ContractService.ItemPrice(101,new BigDecimal("100"),new BigDecimal("85"),new BigDecimal("8.5")))));
        var valid=new java.util.HashMap<String,Object>(Map.of("status","available","usable",1,"amount",BigDecimal.TEN,"minimum_amount",BigDecimal.ZERO,"laboratory_id",2));
        for(String fault:List.of("reserved","expired","laboratory","minimum")){
            var row=new java.util.HashMap<>(valid);
            switch(fault){case "reserved"->row.put("status","reserved");case "expired"->row.put("usable",0);case "laboratory"->row.put("laboratory_id",3);default->row.put("minimum_amount",new BigDecimal("86"));}
            when(store.one(contains("uc.status"),eq(6L),eq(5))).thenReturn(row);
            var error=assertThrows(com.petlis.common.exception.BusinessException.class,()->service.quote(2,List.of(91),6L));
            assertEquals(fault.equals("reserved")?409:400,error.getCode());
        }
    }

}
