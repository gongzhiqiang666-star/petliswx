package com.petlis.service;
import com.petlis.common.dto.request.WechatLoginRequest;
import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.util.*;
import com.petlis.entity.business.User;
import com.petlis.repository.*;
import com.petlis.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
 @Mock UserRepository users;@Mock InfoHospitalRepository hospitals;@Mock JwtUtil jwt;@Mock WechatUtil wechat;@Mock SessionService sessions;
 @InjectMocks AuthServiceImpl service;
 @Test void existingWechatAccountUsesReviewedSession(){var user=new User();user.setId(7);when(wechat.code2Session("code")).thenReturn(new WechatUtil.WechatLoginResponse("openid","private-session-key"));when(users.findByOpenid("openid")).thenReturn(Optional.of(user));var expected=WechatLoginResponse.builder().token("reviewed-session").userId(7).hospitalId(2).build();when(sessions.issue(7)).thenReturn(expected);var request=new WechatLoginRequest();request.setCode("code");assertSame(expected,service.wechatLogin(request));verify(users).save(user);}
 @Test void unknownWechatDoesNotCreateUnreviewedDoctorOrDefaultHospital(){when(wechat.code2Session("code")).thenReturn(new WechatUtil.WechatLoginResponse("unknown","private-session-key"));when(users.findByOpenid("unknown")).thenReturn(Optional.empty());var request=new WechatLoginRequest();request.setCode("code");assertEquals(428,assertThrows(BusinessException.class,()->service.wechatLogin(request)).getCode());verify(users,never()).save(any());verifyNoInteractions(sessions);}
 @Test void refreshChecksLiveSession(){when(sessions.valid("old")).thenReturn(true);when(jwt.getUserIdFromToken("old")).thenReturn(7);when(sessions.issue(7)).thenReturn(WechatLoginResponse.builder().token("new").build());assertEquals("new",service.refreshToken("old"));}
 @Test void revokedSessionCannotRefresh(){when(sessions.valid("old")).thenReturn(false);assertThrows(BusinessException.class,()->service.refreshToken("old"));verify(sessions,never()).issue(anyInt());}
 @Test void logoutRevokesCredentialVersion(){when(sessions.valid("current")).thenReturn(true);when(jwt.getUserIdFromToken("current")).thenReturn(7);service.logout("current");verify(sessions).revoke(7);}
}
