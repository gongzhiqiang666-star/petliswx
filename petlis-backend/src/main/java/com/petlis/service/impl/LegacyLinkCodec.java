package com.petlis.service.impl;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
/** Compatibility codec only; an encrypted legacy ID is never a new-system authentication credential. */
final class LegacyLinkCodec {
 private LegacyLinkCodec(){}
 private static final Pattern WORDS=Pattern.compile("(\\w+)|(\\\\u(\\w{4}))");
 private static byte[] key(String phrase)throws GeneralSecurityException{return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(("DDM"+phrase+"DDM").getBytes(StandardCharsets.UTF_8))).substring(0,8).getBytes(StandardCharsets.US_ASCII);}
 private static Cipher cipher(int mode,String phrase)throws GeneralSecurityException{byte[] key=key(phrase);Cipher cipher=Cipher.getInstance("DES/CBC/PKCS5Padding");cipher.init(mode,new SecretKeySpec(key,"DES"),new IvParameterSpec(key));return cipher;}
 static String encrypt(String value,String phrase){try{return Base64.getEncoder().encodeToString(cipher(Cipher.ENCRYPT_MODE,phrase).doFinal(value.getBytes(StandardCharsets.UTF_8))).replace('+','!').replace('=','-');}catch(GeneralSecurityException e){throw new IllegalStateException("原链接编码不可用",e);}}
 static String decrypt(String token,String phrase){
  if(token==null||token.isEmpty())return "";
  try{byte[] bytes=Base64.getMimeDecoder().decode(token.replace('!','+').replace('-','='));String plain=new String(cipher(Cipher.DECRYPT_MODE,phrase).doFinal(bytes),StandardCharsets.UTF_8);var lower=new StringBuilder();for(char c:plain.toCharArray())lower.append(c>='A'&&c<='Z'?(char)(c+32):c);var m=WORDS.matcher(lower);var output=new StringBuilder();while(m.find()){if(m.group(1)!=null)output.append(m.group(1));else{String hex=m.group(3);int code=hexByte(hex.substring(0,2))+(hexByte(hex.substring(2))<<8);if(code<0xd800||code>0xdfff)if(code!=0xfeff&&code!=0xfffe)output.append((char)code);}}return output.toString();}
  catch(GeneralSecurityException|IllegalArgumentException e){return "";}
 }
 private static int hexByte(String value){String hex=value.replaceAll("[^0-9a-f]","");return hex.isEmpty()?0:Integer.parseInt(hex,16);}
}
