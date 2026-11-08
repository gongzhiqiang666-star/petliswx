package com.petlis.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Transport only. Business services own authorization, persistent attempts and status transitions.
 * No scheduler, startup probe, or configuration save invokes this component.
 */
@Component
public class LegacyMailTransport {
 private final String mode;private final int localPort;private final boolean smtpEnabled;
 public LegacyMailTransport(@Value("${app.legacy-mail.mode:DISABLED}") String mode,@Value("${app.legacy-mail.local-test-port:0}") int localPort,@Value("${app.legacy-mail.smtp-enabled:false}") boolean smtpEnabled){this.mode=mode;this.localPort=localPort;this.smtpEnabled=smtpEnabled;}
 public record Profile(String host,int port,String username,String password,String bcc){@Override public String toString(){return "MailProfile[credentials=REDACTED]";}}
 /** overrideTo models PHP Env::get(app.email): replaces To only; BCC remains configured. */
 public record Message(String to,String overrideTo,String subject,String html,List<Attachment> attachments){}
 public record Attachment(String filename,String contentType,byte[] bytes){
  public Attachment{bytes=bytes==null?null:bytes.clone();}
  @Override public byte[] bytes(){return bytes==null?null:bytes.clone();}
 }
 /** SMTP may accept DATA before a disconnect: callers must not blindly resend this exception. */
 public static final class DeliveryUncertainException extends RuntimeException {
  public DeliveryUncertainException(){super("邮件发送结果未确认，请核对投递记录后处理，勿直接重复发送");}
 }
 public void send(Profile profile,Message command){
  boolean local="TEST_LOCAL".equals(mode);
  if(!local&&!("SMTP".equals(mode)&&smtpEnabled))throw new IllegalStateException("邮件传输尚未开启");
  if(local&&(localPort<1||localPort>65535))throw new IllegalStateException("本机邮件测试端口未配置");
  if(profile==null||command==null)throw new IllegalArgumentException("邮件配置和内容必填");
  header(profile.username());header(command.subject());
  if(command.subject().isEmpty()||command.subject().length()>2000||command.html()==null||command.html().getBytes(StandardCharsets.UTF_8).length>2*1024*1024)throw new IllegalArgumentException("邮件标题或正文无效或过长");
  var from=address(profile.username(),LegacyMailAddressRules.senderName(profile.username()));
  String to=command.overrideTo()==null||command.overrideTo().isEmpty()||command.overrideTo().equals("0")?command.to():command.overrideTo();
  var recipients=addresses(to);var bcc=addresses(profile.bcc());
  List<Attachment> attachments=command.attachments()==null?List.of():List.copyOf(command.attachments());long bytes=0;
  for(var attachment:attachments){
   header(attachment.filename());header(attachment.contentType());
   if(attachment.filename().isBlank()||attachment.filename().length()>250||attachment.filename().contains("/")||attachment.filename().contains("\\")||!attachment.contentType().matches("[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+")||attachment.bytes()==null)throw new IllegalArgumentException("邮件附件无效");
   bytes+=attachment.bytes().length;
  }
  if(attachments.size()>20||bytes>20*1024*1024)throw new IllegalArgumentException("邮件附件过多或过大");
  var sender=new JavaMailSenderImpl();sender.setDefaultEncoding("UTF-8");sender.setProtocol("smtp");
  sender.setHost(local?"127.0.0.1":profile.host());sender.setPort(local?localPort:profile.port());
  var properties=sender.getJavaMailProperties();properties.setProperty("mail.smtp.connectiontimeout","8000");properties.setProperty("mail.smtp.timeout","15000");properties.setProperty("mail.smtp.writetimeout","15000");properties.setProperty("mail.debug","false");
  // Test mode never transmits source SMTP credentials or resolves the configured host.
  if(!local){header(profile.host());if(profile.host().isBlank()||profile.port()<1||profile.port()>65535||profile.password()==null||profile.password().isEmpty())throw new IllegalArgumentException("SMTP 配置无效");sender.setUsername(profile.username());sender.setPassword(profile.password());properties.setProperty("mail.smtp.auth","true");properties.setProperty("mail.smtp.starttls.enable","true");properties.setProperty("mail.smtp.starttls.required","true");properties.setProperty("mail.smtp.ssl.checkserveridentity","true");}
  try{
   var mime=sender.createMimeMessage();var helper=new MimeMessageHelper(mime,attachments.isEmpty()?MimeMessageHelper.MULTIPART_MODE_NO:MimeMessageHelper.MULTIPART_MODE_MIXED,"UTF-8");helper.setFrom(from);helper.setTo(recipients);helper.setBcc(bcc);helper.setSubject(command.subject());helper.setText(command.html(),true);
   for(var attachment:attachments)helper.addAttachment(attachment.filename(),new ByteArrayResource(attachment.bytes()),attachment.contentType());
   sender.send(mime);
  }catch(MailException e){throw new DeliveryUncertainException();}catch(MessagingException e){throw new IllegalArgumentException("邮件内容或附件无法编码");}
 }
 private static void header(String value){if(value==null||value.indexOf('\r')>=0||value.indexOf('\n')>=0||value.indexOf('\0')>=0)throw new IllegalArgumentException("邮件头字段无效");}
 private static InternetAddress address(String email,String name){
  header(email);header(name);
  try{var parsed=new InternetAddress(email,true);parsed.validate();if(!parsed.getAddress().equals(email)||email.isBlank())throw new IllegalArgumentException("邮件地址无效");parsed.setPersonal(name,"UTF-8");return parsed;}catch(MessagingException|java.io.UnsupportedEncodingException e){throw new IllegalArgumentException("邮件地址无效");}
 }
 private static InternetAddress[] addresses(String value){var raw=LegacyMailAddressRules.parse(value);return raw.stream().map(a->address(a.email(),a.name())).toArray(InternetAddress[]::new);}
}
