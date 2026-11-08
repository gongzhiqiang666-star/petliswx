package com.petlis.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.petlis.common.exception.BusinessException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Transport only. No automatic application retries; caller must persist intent before exchange. */
public class LegacyBankGatewayClient {
 enum Mode {DISABLED,TEST_LOCAL,HTTPS}
 static final class UnknownResult extends RuntimeException {UnknownResult(){super("网银请求结果未确认，请查询原交易核对，禁止自动重发");}}
 private final Mode mode;private final URI endpoint;private final String appId,merchant,notifyUrl;private final PrivateKey privateKey;private final PublicKey publicKey;private final Duration timeout;private final HttpClient http;
 LegacyBankGatewayClient(){this(Mode.DISABLED,null,null,null,null,null,null,Duration.ofSeconds(15));}
 LegacyBankGatewayClient(Mode mode,URI endpoint,String appId,String merchant,String notifyUrl,PrivateKey privateKey,PublicKey publicKey,Duration timeout){
  this.mode=mode;this.endpoint=endpoint;this.appId=appId;this.merchant=merchant;this.notifyUrl=notifyUrl;this.privateKey=privateKey;this.publicKey=publicKey;this.timeout=timeout;
  if(mode==null||timeout==null||timeout.isNegative()||timeout.isZero()||timeout.compareTo(Duration.ofSeconds(60))>0||timeout.toMillis()<1)throw new BusinessException(400,"网银通道或超时配置无效");
  if(mode==Mode.DISABLED){http=null;return;}
  if(endpoint==null||endpoint.getHost()==null||endpoint.getUserInfo()!=null||endpoint.getFragment()!=null||endpoint.getPort()>65535||privateKey==null||publicKey==null||!"RSA".equals(privateKey.getAlgorithm())||!(publicKey instanceof java.security.interfaces.RSAPublicKey))throw new BusinessException(400,"网银地址或密钥配置缺失");
  if(mode==Mode.TEST_LOCAL&&(!"http".equals(endpoint.getScheme())||!"127.0.0.1".equals(endpoint.getHost())||endpoint.getPort()<1))throw new BusinessException(400,"本机网银测试仅允许显式127.0.0.1端口");
  if(mode==Mode.HTTPS&&!"https".equals(endpoint.getScheme()))throw new BusinessException(400,"网银实际通道必须为HTTPS");
  http=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(timeout.compareTo(Duration.ofSeconds(5))<0?timeout:Duration.ofSeconds(5)).proxy(new ProxySelector(){@Override public List<Proxy> select(URI uri){return List.of(Proxy.NO_PROXY);}@Override public void connectFailed(URI uri,SocketAddress address,java.io.IOException error){}}).build();
 }
 public boolean available(){return mode!=Mode.DISABLED;}
 public String merchant(){return merchant;}
 public JsonNode verifyNotification(byte[] raw){
  if(mode==Mode.DISABLED)throw new BusinessException(409,"原网银通道尚未开启");
  var body=LegacyBankProtocol.verify(raw,publicKey).path("response").path("msgBody");if(!body.isObject())throw new BusinessException(400,"网银通知格式无效");return body;
 }
 public JsonNode exchange(LegacyBankProtocol.Method method,Map<String,Object> parameters){
  if(mode==Mode.DISABLED)throw new BusinessException(409,"原网银通道尚未开启");if(TransactionSynchronizationManager.isActualTransactionActive())throw new BusinessException(409,"网银请求必须在数据库事务提交后执行");
  var packet=LegacyBankProtocol.request(appId,merchant,notifyUrl,method,parameters,LocalDateTime.now(ZoneId.of("Asia/Shanghai")),privateKey);
  var request=HttpRequest.newBuilder(endpoint).timeout(timeout).header("Content-Type","text/json;charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(packet.form(),StandardCharsets.UTF_8)).build();var receiving=new AtomicReference<LimitedBody>();
  CompletableFuture<HttpResponse<byte[]>> future=http.sendAsync(request,info->{var body=new LimitedBody();receiving.set(body);return body;});
  try{
   var response=future.get(timeout.toMillis(),TimeUnit.MILLISECONDS);if(response.statusCode()!=200)throw new UnknownResult();var verified=LegacyBankProtocol.verify(response.body(),publicKey);var body=verified.path("response").path("msgBody");if(!body.isObject())throw new UnknownResult();return body;
  }catch(InterruptedException e){future.cancel(true);if(receiving.get()!=null)receiving.get().cancel();Thread.currentThread().interrupt();throw new UnknownResult();}
  catch(Exception e){future.cancel(true);if(receiving.get()!=null)receiving.get().cancel();throw new UnknownResult();}
 }
 private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
  private final CompletableFuture<byte[]> body=new CompletableFuture<>();private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();private volatile Flow.Subscription subscription;
  @Override public CompletionStage<byte[]> getBody(){return body;}
  @Override public void onSubscribe(Flow.Subscription value){subscription=value;value.request(1);}
  @Override public void onNext(List<ByteBuffer> chunks){try{for(var chunk:chunks){if((long)bytes.size()+chunk.remaining()>1024*1024){cancel();body.completeExceptionally(new UnknownResult());return;}byte[] data=new byte[chunk.remaining()];chunk.get(data);bytes.write(data);}subscription.request(1);}catch(Exception e){cancel();body.completeExceptionally(new UnknownResult());}}
  @Override public void onError(Throwable error){body.completeExceptionally(error);}
  @Override public void onComplete(){body.complete(bytes.toByteArray());}
  void cancel(){var value=subscription;if(value!=null)value.cancel();}
 }
}
