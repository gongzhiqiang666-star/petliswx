package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LegacyCanonicalRequestTest {
 @Test void sameRequestSurvivesObjectKeyReorderingAcrossProcessRestarts()throws Exception{assertTrue(LegacyCanonicalRequest.same("{\"orderId\":9,\"command\":{\"hospitalId\":7,\"fields\":{\"AnimalName\":\"原名\",\"Age\":\"2岁\"}}}","{\"command\":{\"fields\":{\"Age\":\"2岁\",\"AnimalName\":\"原名\"},\"hospitalId\":7},\"orderId\":9}"));}
 @Test void changedValuesOrArrayOrderAreNotTreatedAsReplay()throws Exception{assertFalse(LegacyCanonicalRequest.same("{\"field\":\"原名\"}","{\"field\":\"另一名字\"}"));assertFalse(LegacyCanonicalRequest.same("{\"items\":[1,2]}","{\"items\":[2,1]}"));assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,()->LegacyCanonicalRequest.same("{\"id\":1}","invalid stored request"));}
 @Test void distinctHighPrecisionNumbersCannotBecomeTheSameRequest()throws Exception{assertFalse(LegacyCanonicalRequest.same("{\"amount\":1.000000000000000001}","{\"amount\":1.000000000000000002}"));}
}
