package com.petlis.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentXmlTest {
    private final WeChatPayHttpClient client=new WeChatPayHttpClient();
    @Test void readsPlainTextAndCdata() {
        var result=client.xmlToMap("<xml><total_fee>1999</total_fee><return_code><![CDATA[SUCCESS]]></return_code></xml>");
        assertEquals("1999",result.get("total_fee"));
        assertEquals("SUCCESS",result.get("return_code"));
    }
    @Test void rejectsExternalEntities() {
        assertThrows(IllegalArgumentException.class,()->client.xmlToMap("<!DOCTYPE xml [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><xml><body>&xxe;</body></xml>"));
    }
    @Test void rejectsAmbiguousDuplicateFields() {
        assertThrows(IllegalArgumentException.class,()->client.xmlToMap("<xml><total_fee>1</total_fee><total_fee>1999</total_fee></xml>"));
    }
    @Test void currencyConversionDoesNotTruncateBinaryFloatingPoint() {
        assertEquals(1999,WeChatPayUtil.yuanToFen(19.99));
        assertThrows(ArithmeticException.class,()->WeChatPayUtil.yuanToFen(1.001));
    }
}
