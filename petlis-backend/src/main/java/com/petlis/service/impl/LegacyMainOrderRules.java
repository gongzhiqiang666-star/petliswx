package com.petlis.service.impl;
import java.util.*;
/** Frozen PHP admin/model/LbListdiagnosisproject.php beforeInsert/beforeUpdate only.
 * Hospital web insertion has a different contract; never apply these defaults to that entry. */
final class LegacyMainOrderRules {
 static Map<String,Object> adminInsert(Map<String,Object> input){
  var row=new LinkedHashMap<String,Object>();if(input!=null)row.putAll(input);
  row.put("UserNO","55550-");row.put("CityId",0);
  if(!phpEmpty(row.get("InfoHospitalId")))row.put("InfoHospitalId",0);
  if(phpEmpty(row.get("InfoAnimalId")))row.put("InfoAnimalId",0);
  if(phpEmpty(row.get("InfoSampleTypeId")))row.put("InfoSampleTypeId",0);
  if(phpEmpty(row.get("InfoSampleTypeId")))row.put("DictSexId",0);
  for(String key:List.of("AnimalName","AnimalName_en","AnimalType_en","AnimalWeight","ChipNO","Age","SampleArea","MasterName","MasterName_en","ReportNO","SaveMedium","Remark"))row.put(key,"");
  for(String key:List.of("AnimalTypeId","AnimalType","TestFee","DeliverySendGerFee","DeliverySendCNFee","InfoCheckReceivedId","IsPayMoney","IsSendNoticeMail","CreateSampleId"))row.put(key,0);
  for(String key:List.of("ReceiveSampleDt","SendSampleDt","SendReportDt"))row.put(key,"1900-01-01");
  return row;
 }
 static Map<String,Object> update(Map<String,Object> input){var row=new LinkedHashMap<String,Object>();if(input!=null)row.putAll(input);for(String key:List.of("SendReportDt","SendSampleDt","Birthday","CollectDt","ReceiveSampleDt"))if(phpEmpty(row.get(key)))row.remove(key);return row;}
 private static boolean phpEmpty(Object value){return value==null||Boolean.FALSE.equals(value)||value instanceof String s&&(s.isEmpty()||s.equals("0"))||value instanceof Number n&&new java.math.BigDecimal(n.toString()).signum()==0||value instanceof Collection<?> c&&c.isEmpty()||value instanceof Map<?,?> m&&m.isEmpty();}
}
