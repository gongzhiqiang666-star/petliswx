package com.petlis.service.impl;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
/** Base.getkuhuirightdatagroup: preview only, not a reservation. StartNO is descriptive, not an arithmetic input. */
final class LegacyGroupNumberRules {
 private static final String NUM="[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?";
 private static final Pattern NUMERIC=Pattern.compile("^[\\x00-\\x20]*"+NUM+"$"),LEADING=Pattern.compile("^[\\x00-\\x20]*("+NUM+")");
 static String preview(String prefix,String latest,long count){
  String word=prefix==null?"":prefix,value=latest==null?"0":latest;
  String candidate=word.isEmpty()?value:increment(value.replace(word,""));
  boolean numericOverflow=!word.isEmpty()&&NUMERIC.matcher(value.replace(word,"")).matches()&&candidate.equals("INF");
  if(!numericOverflow&&!positive(candidate))candidate=BigInteger.valueOf(count).add(BigInteger.ONE).toString();
  int padding=Math.max(0,5-candidate.getBytes(StandardCharsets.UTF_8).length);return word+"0".repeat(padding)+candidate;
 }
 private static boolean positive(String value){var match=LEADING.matcher(value);if(!match.find())return false;try{return Double.parseDouble(match.group(1))>0;}catch(NumberFormatException e){return false;}}
 private static String increment(String value){
  if(value.isEmpty())return "1";
  if(NUMERIC.matcher(value).matches()){
   String clean=value.stripLeading();if(clean.matches("[+-]?[0-9]+")){
    BigInteger integer=new BigInteger(clean);if(integer.compareTo(BigInteger.valueOf(Long.MIN_VALUE))>=0&&integer.compareTo(BigInteger.valueOf(Long.MAX_VALUE))<0)return integer.add(BigInteger.ONE).toString();
   }
   // PHP's non-money counter conversion uses native floating-point for decimal/exponent/overflow strings.
   double number=Double.parseDouble(clean)+1;if(Double.isInfinite(number))return number>0?"INF":"-INF";
   String text=String.format(Locale.ROOT,"%.14G",number),mantissa=text,exponent="";int e=text.indexOf('E');if(e>=0){mantissa=text.substring(0,e);exponent=text.substring(e);}
   if(mantissa.contains("."))mantissa=mantissa.replaceFirst("0+$","").replaceFirst("\\.$","");
   if(!exponent.isEmpty()){if(!mantissa.contains("."))mantissa+=".0";exponent=exponent.replaceFirst("E([+-])0+(?=[0-9])","E$1");}
   return mantissa+exponent;
  }
  char[] chars=value.toCharArray();boolean carry=true;char leading='1';
  for(int i=chars.length-1;i>=0&&carry;i--){char c=chars[i];if(c>='0'&&c<='8'){chars[i]++;carry=false;}else if(c=='9'){chars[i]='0';leading='1';}else if(c>='a'&&c<='y'){chars[i]++;carry=false;}else if(c=='z'){chars[i]='a';leading='a';}else if(c>='A'&&c<='Y'){chars[i]++;carry=false;}else if(c=='Z'){chars[i]='A';leading='A';}else carry=false;}
  return (carry?String.valueOf(leading):"")+new String(chars);
 }
}
