package com.petlis.controller;
import com.petlis.common.util.JwtUtil;
import com.petlis.service.impl.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;
/** Controller contract tests use a validated fixture token; database session checks have separate policy and integration tests. */
abstract class ControllerSecurityTestSupport {
 @MockBean JwtUtil jwt;
 @MockBean SessionService sessions;
 @BeforeEach void authenticatedFixture(){when(sessions.valid("test_token_123")).thenReturn(true);when(jwt.getUserIdFromToken("test_token_123")).thenReturn(7);when(jwt.getHospitalIdFromToken("test_token_123")).thenReturn(1);when(jwt.getCityIdFromToken("test_token_123")).thenReturn(1);}
}
