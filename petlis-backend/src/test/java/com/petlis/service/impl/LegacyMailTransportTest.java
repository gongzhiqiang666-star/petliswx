package com.petlis.service.impl;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMailTransportTest {
 LegacyMailTransport.Profile profile(){return new LegacyMailTransport.Profile("must-not-resolve.invalid",587,"sender@laboklin.cn","synthetic-password","audit@example.test");}
 LegacyMailTransport.Message message(){return new LegacyMailTransport.Message("doctor@example.test;doctor@example.test",null,"中文报告 α","<p>报告正文 中文 α</p>",List.of(new LegacyMailTransport.Attachment("检测报告.pdf","application/pdf","%PDF-synthetic".getBytes(StandardCharsets.UTF_8))));}
 @Test void disabledTransportFailsBeforeConnecting(){try(var sink=new Sink()){
  var sender=new LegacyMailTransport("DISABLED",sink.port(),false);
  assertThrows(IllegalStateException.class,()->sender.send(profile(),message()));assertEquals(0,sink.connections);
 }}
 @Test void localSmtpReceivesDeduplicatedRecipientsBccChineseBodyAndAttachment() throws Exception {
  try(var sink=new Sink()){
   new LegacyMailTransport("TEST_LOCAL",sink.port(),false).send(profile(),message());
   MimeMessage received=sink.received.get(8,TimeUnit.SECONDS);
   assertEquals("中文报告 α",received.getSubject());assertEquals("sender",((InternetAddress)received.getFrom()[0]).getPersonal());
   assertEquals(List.of("<doctor@example.test>","<audit@example.test>"),sink.recipients);
   assertNull(received.getHeader("Bcc"));var parts=(Multipart)received.getContent();
   assertTrue(parts.getBodyPart(0).getContent().toString().contains("报告正文 中文 α"));assertEquals("检测报告.pdf",parts.getBodyPart(1).getFileName());assertArrayEquals("%PDF-synthetic".getBytes(StandardCharsets.UTF_8),parts.getBodyPart(1).getInputStream().readAllBytes());
   assertFalse(sink.commands.stream().anyMatch(s->s.startsWith("AUTH")));assertEquals(1,sink.connections);
  }
 }
 @Test void invalidAddressAndHeaderInjectionFailBeforeAnyConnection(){try(var sink=new Sink()){
  var sender=new LegacyMailTransport("TEST_LOCAL",sink.port(),false);
  assertThrows(IllegalArgumentException.class,()->sender.send(profile(),new LegacyMailTransport.Message("not-an-address",null,"subject","body",List.of())));
  assertThrows(IllegalArgumentException.class,()->sender.send(profile(),new LegacyMailTransport.Message("a@example.test",null,"subject\r\nBcc:bad@example.test","body",List.of())));
  assertEquals(0,sink.connections);
 }}
 @Test void smtpCannotBeEnabledByModeAloneAndLocalModeNeedsExplicitPort(){
  assertThrows(IllegalStateException.class,()->new LegacyMailTransport("SMTP",2525,false).send(profile(),message()));
  assertThrows(IllegalStateException.class,()->new LegacyMailTransport("TEST_LOCAL",0,false).send(profile(),message()));
 }
 @Test void sourceEnvironmentOverrideReplacesToButKeepsConfiguredBcc() throws Exception {
  try(var sink=new Sink()){
   var original=message();new LegacyMailTransport("TEST_LOCAL",sink.port(),false).send(profile(),new LegacyMailTransport.Message(original.to(),"sandbox@example.test",original.subject(),original.html(),List.of()));
   sink.received.get(8,TimeUnit.SECONDS);assertEquals(List.of("<sandbox@example.test>","<audit@example.test>"),sink.recipients);
  }
 }
 @Test void reportOverrideDoesNotLeakToOriginalBcc() throws Exception {
  try(var sink=new Sink()){
   var original=message();new LegacyMailTransport("TEST_LOCAL",sink.port(),false).send(profile(),new LegacyMailTransport.Message(" original@example.test ","sandbox@example.test",original.subject(),original.html(),List.of()),LegacyMailRoutingRules.Kind.REPORT);
   sink.received.get(8,TimeUnit.SECONDS);assertFalse(sink.recipients.isEmpty());assertTrue(sink.recipients.stream().allMatch(address->address.equals("<sandbox@example.test>")));
  }
 }
 @Test void smtpDataFailureIsUncertainAndDoesNotAutomaticallyResend(){
  try(var sink=new Sink(true)){
   assertThrows(LegacyMailTransport.DeliveryUncertainException.class,()->new LegacyMailTransport("TEST_LOCAL",sink.port(),false).send(profile(),message()));
   assertEquals(1,sink.connections);
  }
 }
 /** Own ephemeral loopback fixture only; never binds a known service port. */
 static final class Sink implements AutoCloseable {
  final ServerSocket server;final ExecutorService executor=Executors.newSingleThreadExecutor();final CompletableFuture<MimeMessage> received=new CompletableFuture<>();final List<String> recipients=new ArrayList<>(),commands=new ArrayList<>();volatile int connections;
  Sink(){this(false);}
  Sink(boolean rejectData){try{server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));server.setSoTimeout(10000);}catch(IOException e){throw new UncheckedIOException(e);}executor.submit(()->{try(var socket=server.accept()){connections++;socket.setSoTimeout(8000);var reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));var out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.US_ASCII),true);out.print("220 local-test SMTP\r\n");out.flush();String line;while((line=reader.readLine())!=null){commands.add(line);if(line.startsWith("EHLO")||line.startsWith("HELO"))out.print("250 local-test\r\n");else if(line.startsWith("MAIL FROM:"))out.print("250 OK\r\n");else if(line.startsWith("RCPT TO:")){recipients.add(line.substring(8));out.print("250 OK\r\n");}else if(line.equals("DATA")){out.print("354 End with dot\r\n");out.flush();var body=new StringBuilder();while((line=reader.readLine())!=null&&!line.equals("."))body.append(line.startsWith("..")?line.substring(1):line).append("\r\n");received.complete(new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.US_ASCII))));out.print(rejectData?"554 synthetic rejection\r\n":"250 queued locally\r\n");}else if(line.equals("QUIT")){out.print("221 bye\r\n");out.flush();break;}else out.print("250 OK\r\n");out.flush();}}catch(Exception e){received.completeExceptionally(e);}});}
  int port(){return server.getLocalPort();}
  public void close(){try{server.close();}catch(IOException ignored){}executor.shutdownNow();}
 }
}
