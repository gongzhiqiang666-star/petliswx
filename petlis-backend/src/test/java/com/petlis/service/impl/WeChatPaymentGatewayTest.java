package com.petlis.service.impl;
import com.petlis.config.WeChatPayConfig;
import com.petlis.common.exception.BusinessException;
import com.petlis.util.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WeChatPaymentGatewayTest {
 WeChatPayConfig config;WeChatPayHttpClient http;WeChatPaymentGateway gateway;
 @BeforeEach void setup(){config=new WeChatPayConfig();config.setMockEnabled(false);config.setAppId("test-app");config.setMchId("test-merchant");config.setApiKey("unit-test-key-never-used-for-live");config.setNotifyUrl("https://example.test/api/payment/notify");http=spy(new WeChatPayHttpClient());gateway=new WeChatPaymentGateway(config,http);}
 Map<String,String> response(){return new HashMap<>(Map.of("return_code","SUCCESS","result_code","SUCCESS","appid","test-app","mch_id","test-merchant","nonce_str","fixture","prepay_id","verified-prepay"));}
 void provider(Map<String,String> response){doReturn(http.mapToXml(response)).when(http).postXml(anyString(),anyString());}
 @Test void unsignedProviderResponseCannotProducePayment(){provider(response());assertThrows(BusinessException.class,()->gateway.create("RC-fixture",new BigDecimal("20.00"),"test-openid","fixture"));}
 @Test void verifiedProviderResponseProducesExpectedPrepay(){var r=response();r.put("sign",WeChatPayUtil.generateMD5Sign(r,config.getApiKey()));provider(r);assertEquals("verified-prepay",gateway.create("RC-fixture",new BigDecimal("20.00"),"test-openid","fixture"));}
 @Test void anotherMerchantResponseIsRejected(){var r=response();r.put("mch_id","other-merchant");r.put("sign",WeChatPayUtil.generateMD5Sign(r,config.getApiKey()));provider(r);assertThrows(BusinessException.class,()->gateway.create("RC-fixture",new BigDecimal("20.00"),"test-openid","fixture"));}
 @Test void xmlSerializerPreservesUntrustedDescription(){var client=new WeChatPayHttpClient();var values=Map.of("body","Pet ]]> <injected>content</injected>");assertEquals(values,client.xmlToMap(client.mapToXml(values)));}
}
