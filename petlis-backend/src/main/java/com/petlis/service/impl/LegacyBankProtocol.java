package com.petlis.service.impl;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.petlis.common.exception.BusinessException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Source gnete Client/Signature wire contract. Pure codec: no network, files or database. */
final class LegacyBankProtocol {
 private static final int MAX_BYTES=1024*1024;
 private static final ObjectMapper JSON=new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()).enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
 enum Method {PREPAY("prePay"),PAY_QUERY("payQuery"),REFUND("refund"),REFUND_QUERY("refundQuery");final String wire;Method(String wire){this.wire=wire;}}
 record SignedRequest(String businessJson,String signingText,String signatureHex,String form){@Override public String toString(){return "BankSignedRequest[REDACTED]";}}
 static SignedRequest request(String appId,String merchant,String notifyUrl,Method method,Map<String,Object> parameters,LocalDateTime time,PrivateKey key){
  for(String v:new String[]{appId,merchant,notifyUrl})if(v==null||v.isBlank()||v.length()>2000)throw bad("原网银配置缺失或无效");if(method==null||parameters==null||parameters.isEmpty()||time==null||key==null||!"RSA".equals(key.getAlgorithm()))throw bad("原网银请求参数或RSA密钥无效");
  var business=new LinkedHashMap<String,Object>();business.put("sndDt",time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));business.put("busiMerNo",merchant);business.put("msgBody",parameters);business.put("notifyUrl",notifyUrl);business.put("remark","");String json=phpJson(business);
  var fields=new LinkedHashMap<String,String>();fields.put("app_id",appId);fields.put("method","gnete.upbc.cashier.trade."+method.wire);fields.put("timestamp",time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));fields.put("v","1.0.1");fields.put("sign_alg","1");fields.put("biz_content",json);String unsigned=form(fields);
  try{var signature=Signature.getInstance("SHA256withRSA");signature.initSign(key);signature.update(unsigned.getBytes(StandardCharsets.UTF_8));String hex=HexFormat.of().formatHex(signature.sign());fields.put("sign",hex);return new SignedRequest(json,unsigned,hex,form(fields));}catch(GeneralSecurityException e){throw bad("原网银请求签名失败");}
 }
 private static String form(Map<String,String> fields){var parts=new ArrayList<String>();for(var field:fields.entrySet())parts.add(url(field.getKey())+"="+url(field.getValue()));return String.join("&",parts);}
 private static String url(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("*","%2A");}
 static String phpJson(Object value){var out=new StringBuilder();append(out,value,0);if(out.length()>MAX_BYTES)throw bad("原网银请求过大");return out.toString();}
 private static void append(StringBuilder out,Object value,int depth){
  if(depth>64||out.length()>MAX_BYTES)throw bad("原网银请求结构过大");
  if(value==null)out.append("null");else if(value instanceof String v)string(out,v);else if(value instanceof Boolean||value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long||value instanceof BigInteger)out.append(value);
  else if(value instanceof Map<?,?> map){if(map.isEmpty()){out.append("[]");return;}out.append('{');boolean first=true;for(var entry:map.entrySet()){if(!(entry.getKey() instanceof String name))throw bad("原网银对象字段名必须为文本");if(!first)out.append(',');first=false;string(out,name);out.append(':');append(out,entry.getValue(),depth+1);}out.append('}');}
  else if(value instanceof List<?> list){out.append('[');for(int i=0;i<list.size();i++){if(i>0)out.append(',');append(out,list.get(i),depth+1);}out.append(']');}
  else throw bad("原网银参数须使用文本金额、整数或JSON结构，不能传入浮点金额");
 }
 private static void string(StringBuilder out,String value){out.append('"');for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){
  case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '/'->out.append("\\/");case '\b'->out.append("\\b");case '\f'->out.append("\\f");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");case '<'->out.append("\\u003C");case '>'->out.append("\\u003E");
  default->{if(Character.isHighSurrogate(c)){if(i+1>=value.length()||!Character.isLowSurrogate(value.charAt(i+1)))throw bad("原网银文本包含无效Unicode");unicode(out,c);unicode(out,value.charAt(++i));}else if(Character.isLowSurrogate(c))throw bad("原网银文本包含无效Unicode");else if(c<32||c>127)unicode(out,c);else out.append(c);}
 }}out.append('"');}
 private static void unicode(StringBuilder out,char c){out.append("\\u");String hex=Integer.toHexString(c);out.append("0".repeat(4-hex.length())).append(hex);}
 static JsonNode verify(byte[] utf8,PublicKey key){
  if(utf8==null||utf8.length==0||utf8.length>MAX_BYTES||!(key instanceof RSAPublicKey rsa))throw bad("原网银响应或RSA公钥无效");
  try{
   String raw=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(utf8)).toString();int offset=-1;String hex=null;
   try(var parser=JSON.getFactory().createParser(raw)){
    if(parser.nextToken()!=JsonToken.START_OBJECT)throw bad("原网银响应必须是JSON对象");while(parser.nextToken()!=JsonToken.END_OBJECT){if(parser.currentToken()!=JsonToken.FIELD_NAME)throw bad("原网银响应结构无效");String name=parser.currentName();int fieldOffset=Math.toIntExact(parser.currentTokenLocation().getCharOffset());JsonToken value=parser.nextToken();
     if("sign".equals(name)){if(value!=JsonToken.VALUE_STRING)throw bad("原网银响应签名无效");hex=parser.getText();offset=fieldOffset;if(parser.nextToken()!=JsonToken.END_OBJECT)throw bad("原网银签名必须是最后一个顶层字段");break;}parser.skipChildren();
    }if(parser.nextToken()!=null)throw bad("原网银响应包含未签名的额外内容");
   }
   int bytes=(rsa.getModulus().bitLength()+7)/8;if(hex==null||hex.length()!=bytes*2||!hex.matches("[0-9a-fA-F]+"))throw bad("原网银响应签名缺失或格式无效");
   int comma=offset-1;while(comma>=0&&Character.isWhitespace(raw.charAt(comma)))comma--;if(comma<0||raw.charAt(comma)!=',')throw bad("原网银响应缺少签名原文");String signed=raw.substring(0,comma)+"}";
   var verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(key);verifier.update(signed.getBytes(StandardCharsets.UTF_8));if(!verifier.verify(HexFormat.of().parseHex(hex)))throw bad("原网银响应验签失败");
   var result=(ObjectNode)JSON.readTree(raw);result.remove("sign");return result;
  }catch(BusinessException e){throw e;}catch(Exception e){throw bad("原网银响应无法验证");}
 }
 private static BusinessException bad(String message){return new BusinessException(400,message);}
}
