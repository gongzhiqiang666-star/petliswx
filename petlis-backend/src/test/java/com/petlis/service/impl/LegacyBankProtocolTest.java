package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.time.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyBankProtocolTest {
 @TempDir Path directory;
 KeyPair keys()throws Exception{var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);return generator.generateKeyPair();}
 @Test void sourceJsonAndFormEncodingRetainTypesOrderingAndPhpEscapes()throws Exception{
  var fields=new LinkedHashMap<String,Object>();fields.put("merOrdrNo","000123");fields.put("trxTtlAmt","1250");fields.put("subject","中文 <a>&'*~ / 😀");fields.put("userNo",7);
  var request=LegacyBankProtocol.request("test-app","test-merchant","https://callback.example.test/notify",LegacyBankProtocol.Method.PREPAY,fields,LocalDateTime.of(2026,11,8,12,34,56),keys().getPrivate());
  String expected="{\"sndDt\":\"20261108123456\",\"busiMerNo\":\"test-merchant\",\"msgBody\":{\"merOrdrNo\":\"000123\",\"trxTtlAmt\":\"1250\",\"subject\":\"\\u4e2d\\u6587 \\u003Ca\\u003E&'*~ \\/ \\ud83d\\ude00\",\"userNo\":7},\"notifyUrl\":\"https:\\/\\/callback.example.test\\/notify\",\"remark\":\"\"}";
  assertEquals(expected,request.businessJson());assertTrue(request.signingText().startsWith("app_id=test-app&method=gnete.upbc.cashier.trade.prePay&timestamp=2026-11-08+12%3A34%3A56&v=1.0.1&sign_alg=1&biz_content="));assertTrue(request.signingText().contains("%2A%7E"));assertEquals(request.signingText()+"&sign="+request.signatureHex(),request.form());assertEquals(512,request.signatureHex().length());assertFalse(request.toString().contains("000123"));
 }
 String sign(String raw,PrivateKey key)throws Exception{var signature=Signature.getInstance("SHA256withRSA");signature.initSign(key);signature.update(raw.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(signature.sign());}
 String envelope(String unsigned,PrivateKey key)throws Exception{return unsigned.substring(0,unsigned.length()-1)+",\"sign\":\""+sign(unsigned,key)+"\"}";}
 @Test void verifiesOriginalBytesWithWhitespaceNestedSignWordsAndUnicode()throws Exception{
  var key=keys();String raw="{\"response\" : {\"msgBody\":{\"tradeStatus\":\"00\",\"subject\":\"design 中文\",\"nested\":{\"sign\":\"data only\"}}},\"retCode\":\"00000\"}";
  var verified=LegacyBankProtocol.verify(envelope(raw,key.getPrivate()).getBytes(StandardCharsets.UTF_8),key.getPublic());assertEquals("design 中文",verified.path("response").path("msgBody").path("subject").asText());assertEquals("data only",verified.path("response").path("msgBody").path("nested").path("sign").asText());assertFalse(verified.has("sign"));
  assertThrows(BusinessException.class,()->LegacyBankProtocol.verify(envelope(raw,key.getPrivate()).replace("00","01").getBytes(StandardCharsets.UTF_8),key.getPublic()));
 }
 @Test void rejectsWrongKeysDuplicateFieldsMalformedHexAndUnsignedTail()throws Exception{
  var key=keys();String raw="{\"response\":{\"msgBody\":{\"tradeStatus\":\"00\"}}}",good=envelope(raw,key.getPrivate());assertThrows(BusinessException.class,()->LegacyBankProtocol.verify(good.getBytes(StandardCharsets.UTF_8),keys().getPublic()));
  for(String invalid:List.of("{\"sign\":\"zz\"}",good+"{}",good.substring(0,good.length()-1)+",\"extra\":1}",envelope("{\"response\":1,\"response\":2}",key.getPrivate()),good.replace("\"sign\":\"","\"sign\":\"zz"))){assertThrows(BusinessException.class,()->LegacyBankProtocol.verify(invalid.getBytes(StandardCharsets.UTF_8),key.getPublic()));}
  assertThrows(BusinessException.class,()->LegacyBankProtocol.verify(new byte[]{(byte)0xff},key.getPublic()));
 }
 @Test void interoperatesWithIndependentOpenSslSignerAndVerifier()throws Exception{
  var key=keys();Path privateFile=directory.resolve("test-private.pem"),publicFile=directory.resolve("test-public.pem"),message=directory.resolve("message.txt"),signature=directory.resolve("signature.bin");
  Files.writeString(privateFile,pem("PRIVATE KEY",key.getPrivate().getEncoded()));Files.writeString(publicFile,pem("PUBLIC KEY",key.getPublic().getEncoded()));String raw="{\"response\":{\"msgBody\":{\"tradeStatus\":\"00\",\"trxTtlAmt\":\"1250\"}}}";Files.writeString(message,raw);
  var p=new ProcessBuilder("/usr/bin/openssl","dgst","-sha256","-sign",privateFile.toString(),"-out",signature.toString(),message.toString()).redirectErrorStream(true).start();if(!p.waitFor(10,java.util.concurrent.TimeUnit.SECONDS)){p.destroyForcibly();fail("Owned OpenSSL process timed out");}assertEquals(0,p.exitValue());String signed=raw.substring(0,raw.length()-1)+",\"sign\":\""+HexFormat.of().formatHex(Files.readAllBytes(signature))+"\"}";assertEquals("1250",LegacyBankProtocol.verify(signed.getBytes(StandardCharsets.UTF_8),key.getPublic()).path("response").path("msgBody").path("trxTtlAmt").asText());
  Files.write(signature,HexFormat.of().parseHex(sign(raw,key.getPrivate())));p=new ProcessBuilder("/usr/bin/openssl","dgst","-sha256","-verify",publicFile.toString(),"-signature",signature.toString(),message.toString()).redirectErrorStream(true).start();if(!p.waitFor(10,java.util.concurrent.TimeUnit.SECONDS)){p.destroyForcibly();fail("Owned OpenSSL process timed out");}assertEquals(0,p.exitValue());
 }
 String pem(String label,byte[] data){return "-----BEGIN "+label+"-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(data)+"\n-----END "+label+"-----\n";}
}
