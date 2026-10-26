package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.ReviewPermission;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotificationPolicyTest {
 @Test void disabledPushDoesNotPretendSent(){var service=new SubscriptionPush(new MockEnvironment(),new ObjectMapper());var r=service.send("order","test","审核通过","需要支付","pages/orders/orders");assertEquals("unconfigured",r.status());assertEquals("NOT_CONFIGURED",r.code());}
 @Test void unicodeTemplateLengthRespectsCodePoints(){assertEquals(20,SubscriptionPush.shortText("🐱".repeat(30)).codePointCount(0,SubscriptionPush.shortText("🐱".repeat(30)).length()));}
 @Test void legacyOrderOnlyNotifiesUsersOfItsOwnHospital(){var jdbc=mock(JdbcTemplate.class);var store=new FeatureStore(jdbc,new ObjectMapper());when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("id",7)));var service=new BusinessNotifications(store,mock(SubscriptionPush.class),mock(ReviewPermission.class),mock(TransactionTemplate.class),new MockEnvironment(),new ObjectMapper());service.order(10,0,1);verify(jdbc).queryForList(contains("o.InfoHospitalId=u.hospital_id"),eq(10));verify(jdbc).update(contains("INSERT INTO lin_notification_event"),eq(7),eq("order"),anyString(),eq("送检单审核未通过"),contains("审核未通过"),eq("pages/order-detail/order-detail?id=10"));}
}
