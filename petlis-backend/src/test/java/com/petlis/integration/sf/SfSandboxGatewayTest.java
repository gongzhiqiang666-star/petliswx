package com.petlis.integration.sf;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SfSandboxGatewayTest {
 @Test void platformSuccessIsNotBusinessSuccess() throws Exception {
  var gateway=new SfSandboxGateway("fixture","not-a-secret",new ObjectMapper(),form->"{\"apiResultCode\":\"A1000\",\"apiResultData\":\"{\\\"success\\\":false,\\\"errorCode\\\":\\\"6118\\\"}\"}");
  assertFalse(gateway.call("EXP_RECE_CREATE_ORDER",Map.of(),"00000000-0000-0000-0000-000000000001",1).success());
 }
 @Test void transportFailureNeverRetriesCreate(){
  var calls=new AtomicInteger();
  var gateway=new SfSandboxGateway("fixture","not-a-secret",new ObjectMapper(),form->{calls.incrementAndGet();throw new java.io.IOException("timeout");});
  assertThrows(java.io.IOException.class,()->gateway.call("EXP_RECE_CREATE_ORDER",Map.of("orderId","synthetic"),"00000000-0000-0000-0000-000000000001",1));
  assertEquals(1,calls.get());
 }

 @Test void cancellationRequiresOperationSuccess() throws Exception {
  var gateway=new SfSandboxGateway("fixture","not-a-secret",new ObjectMapper(),form->"{\"apiResultCode\":\"A1000\",\"apiResultData\":{\"success\":true,\"errorCode\":\"S0000\",\"msgData\":{\"resStatus\":1}}}");
  assertFalse(gateway.call("EXP_RECE_UPDATE_ORDER",Map.of("dealType",2),"00000000-0000-0000-0000-000000000001",1).success());
 }
 @Test void acceptedCancellationIsReadable() throws Exception {
  var gateway=new SfSandboxGateway("fixture","not-a-secret",new ObjectMapper(),form->"{\"apiResultCode\":\"A1000\",\"apiResultData\":{\"success\":true,\"errorCode\":\"S0000\",\"msgData\":{\"resStatus\":2}}}");
  assertTrue(gateway.call("EXP_RECE_UPDATE_ORDER",Map.of("dealType",2),"00000000-0000-0000-0000-000000000001",1).success());
 }
 @Test void cloudPrintUsesItsOwnDocumentedSuccessEnvelope() throws Exception {
  var gateway=new SfSandboxGateway("fixture","fixture",new ObjectMapper(),form->"{\"apiResultCode\":\"A1000\",\"apiResultData\":{\"success\":true,\"obj\":{\"fileType\":\"pdf\",\"files\":[{}]}}}");
  assertTrue(gateway.call("COM_RECE_CLOUD_PRINT_WAYBILLS",Map.of(),"00000000-0000-0000-0000-000000000001",1).success());
 }
 @Test void cloudPrintAcceptanceWithoutFilesIsNotAReadyPdf() throws Exception {
  var gateway=new SfSandboxGateway("fixture","fixture",new ObjectMapper(),form->"{\"apiResultCode\":\"A1000\",\"apiResultData\":{\"success\":true}} ");
  assertFalse(gateway.call("COM_RECE_CLOUD_PRINT_WAYBILLS",Map.of(),"00000000-0000-0000-0000-000000000001",1).success());
 }
}
