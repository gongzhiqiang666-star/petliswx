package com.petlis.service.impl;

import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserRoleService;
import com.petlis.common.util.JwtUtil;
import com.petlis.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionPolicyTest {
 JdbcTemplate jdbc; FeatureStore store; JwtUtil jwt; ReviewPermission permission; SessionService sessions;
 @BeforeEach void setup(){
  jdbc=mock(JdbcTemplate.class);var target=new FeatureStore(jdbc,new ObjectMapper());
  var factory=new org.springframework.aop.framework.ProxyFactory(target);factory.setProxyTargetClass(true);
  factory.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> invocation.proceed());
  store=(FeatureStore)factory.getProxy();jwt=new JwtUtil();
  ReflectionTestUtils.setField(jwt,"secret","unit-test-secret-only-0123456789abcdef0123456789abcdef");ReflectionTestUtils.setField(jwt,"expiration",60000L);
  permission=new ReviewPermission();ReflectionTestUtils.setField(permission,"adminUserIds","42");sessions=new SessionService(store,jwt,permission,mock(UserRoleService.class));
 }
 Map<String,Object> user(Integer hospital,int version){var u=new HashMap<String,Object>();u.put("status","normal");u.put("review_status","approved");u.put("credential_version",version);u.put("hospital_id",hospital);return u;}
 void returned(Map<String,Object> u){when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(u));}
 @Test void reviewerSessionHasNoHospitalAndDedicatedRole(){var u=user(null,2);returned(u);var session=sessions.issue(42);assertTrue(session.isReviewer());assertNull(session.getHospitalId());assertTrue(jwt.isReviewerToken(session.getToken()));assertTrue(session.getRoles().contains("REVIEWER"));assertFalse(session.getRoles().contains("ADMIN"));assertTrue(sessions.valid(session.getToken()));}
 @Test void reviewerCannotUseDoctorRoleToken(){returned(user(9,0));assertFalse(sessions.valid(jwt.generateToken(42,null,9,1)));}
 @Test void ordinaryDoctorCannotUseReviewerToken(){returned(user(9,0));assertFalse(sessions.valid(jwt.generateReviewerToken(7,0)));}
 @Test void revokedCredentialVersionInvalidatesToken(){returned(user(9,2));assertFalse(sessions.valid(jwt.generateToken(7,null,9,1,1)));}
 @Test void missingHospitalClaimDoesNotThrow(){returned(user(9,0));assertFalse(sessions.valid(jwt.generateToken(7,null,null,1)));}
 @Test void pendingDoctorCannotGetSession(){var u=user(9,0);u.put("review_status","pending");returned(u);assertThrows(BusinessException.class,()->sessions.issue(7));}
}
