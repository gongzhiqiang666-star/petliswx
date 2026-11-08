package com.petlis.integration.sf;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SfRoutePushParserTest {
 final ObjectMapper json=new ObjectMapper();final SfRoutePushParser parser=new SfRoutePushParser(json);
 ObjectNode route(){return json.createObjectNode().put("mailno","SF0000000000001").put("orderid","PETLIS_QA_1").put("acceptTime","2026-11-07 12:30:00").put("opCode","50").put("acceptAddress","沙箱测试城市").put("remark","合成揽收事件").put("firstStatusCode","1");}
 String body(Object...nodes)throws Exception{return json.writeValueAsString(Map.of("Body",Map.of("WaybillRoute",nodes)));}
 @Test void officialEnvelopeNormalizesExistingRouteFieldsWithoutSideEffects()throws Exception {
  var routes=parser.parse(body(route()));assertEquals(1,routes.size());var first=routes.get(0);
  assertEquals("SF0000000000001",first.trackingNo());assertEquals("PETLIS_QA_1",first.externalOrderId());
  assertEquals("1",first.event().path("firstStatusCode").asText());assertEquals("合成揽收事件",first.event().path("remark").asText());
 }
 @Test void wholeBatchRejectsInvalidNodeAndWrongEnvelope()throws Exception {
  assertThrows(BusinessException.class,()->parser.parse(body(route(),route().put("acceptTime","2026-02-30 12:30:00"))));
  assertThrows(BusinessException.class,()->parser.parse("{\"WaybillRoute\":[]}"));
  assertThrows(BusinessException.class,()->parser.parse(body()));
  assertThrows(BusinessException.class,()->parser.parse(body(Collections.nCopies(11,route()).toArray())));
 }
 @Test void ambiguousJsonAndWrongTypesCannotBeAccepted()throws Exception {
  assertThrows(BusinessException.class,()->parser.parse("{\"Body\":{},\"Body\":{\"WaybillRoute\":[]}}"));
  assertThrows(BusinessException.class,()->parser.parse(body(route())+" {}"));
  assertThrows(BusinessException.class,()->parser.parse(body(route().put("opCode",50))));
  assertThrows(BusinessException.class,()->parser.parse(body(route().put("mailno","https://outside.invalid"))));
 }
 @Test void payloadBoundsAndUnknownStatusAreExplicit()throws Exception {
  assertThrows(BusinessException.class,()->parser.parse(" ".repeat(262145)));
  assertThrows(BusinessException.class,()->parser.parse(body(route().put("remark","x".repeat(1001)))));
  assertEquals("new-status",parser.parse(body(route().put("firstStatusCode","new-status"))).get(0).event().path("firstStatusCode").asText());
  assertThrows(BusinessException.class,()->parser.parse(body(route().put("orderid","bad\norder"))));
 }
 @Test void callerMapperSettingsAreNotChanged(){assertFalse(json.isEnabled(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION));}
}
