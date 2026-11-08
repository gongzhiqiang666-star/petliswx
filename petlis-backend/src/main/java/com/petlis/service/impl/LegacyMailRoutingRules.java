package com.petlis.service.impl;
import java.util.Objects;
/** Source-specific routing: Base::sendmail, LbListdiagnosisproject::sendmail,
 * Hospmonthpeport::send. The report override intentionally differs from finance.
 */
public final class LegacyMailRoutingRules {
 private LegacyMailRoutingRules(){}
 public enum Kind { REPORT, SAMPLE_NOTICE, FINANCE }
 public record Route(String to,String bcc){}
 public static Route route(Kind kind,String contact,String environmentOverride,String configuredBcc){
  Objects.requireNonNull(kind,"邮件业务类型必填");
  String recipient=kind==Kind.REPORT?phpTrim(contact==null?"":contact):contact;
  boolean override=environmentOverride!=null&&!environmentOverride.isEmpty()&&!environmentOverride.equals("0");
  return new Route(override?environmentOverride:recipient,override&&kind==Kind.REPORT?environmentOverride:configuredBcc);
 }
 private static String phpTrim(String value){int from=0,to=value.length();while(from<to&&trimCharacter(value.charAt(from)))from++;while(to>from&&trimCharacter(value.charAt(to-1)))to--;return value.substring(from,to);}
 private static boolean trimCharacter(char c){return c==' '||c=='\t'||c=='\n'||c=='\r'||c=='\0'||c=='\u000b';}
}
