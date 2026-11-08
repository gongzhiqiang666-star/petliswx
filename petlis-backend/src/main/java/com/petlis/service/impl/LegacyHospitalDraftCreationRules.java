package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import java.time.*;
import java.util.*;
/** Frozen index/model/LbListdiagnosisproject::beforeInsert, NOT the admin model. */
final class LegacyHospitalDraftCreationRules {
 // PHP strtotime applies year/month offsets together, then normalizes overflowing days.
 // Nonstandard signed/exponent age inputs remain gated pending a PHP-runtime differential check.
 static LocalDate birthday(String age,LocalDate today){
  if(age==null||age.isBlank()||age.matches("\\s*-\\s*岁\\s*-\\s*月\\s*"))return today;
  if(!age.matches("\\s*\\d{1,3}(?:\\s*岁\\s*\\d{1,3}\\s*月?)?\\s*"))throw new BusinessException(409,"该年龄格式尚未完成原系统换算核对，请使用数字岁月格式，例如2岁3月");
  String[] parts=age.trim().split("岁",-1);long years=Long.parseLong(parts[0].trim()),months=parts.length==1?0:Long.parseLong(parts[1].replace("月","").trim());
  try{var result=today.withDayOfMonth(1).minusYears(years).minusMonths(months).plusDays(today.getDayOfMonth()-1L);if(result.getYear()<1000)throw new DateTimeException("out of database range");return result;}catch(DateTimeException error){throw new BusinessException(400,"推算生日超出原数据库日期范围");}
 }
 static Map<String,Object> defaults(Map<String,Object> input,int hospital,LocalDate today,Map<String,Object> breed){
  var row=new LinkedHashMap<String,Object>(input);row.put("InfoHospitalId",hospital);row.put("CreateDataTypeIId",2);row.put("SampleStatus",-2);row.putIfAbsent("Age","");row.put("Birthday",birthday(Objects.toString(row.get("Age"),""),today));
  for(String key:List.of("InfoAnimalId","InfoSampleTypeId","DictSexId","AnimalTypeId"))row.putIfAbsent(key,0);
  for(String key:List.of("AnimalName","AnimalName_en","MasterName","DoctorName","MarketingName"))row.putIfAbsent(key,"");
  row.put("AnimalType",breed.getOrDefault("Variety_CN",""));row.put("AnimalType_en",breed.getOrDefault("Variety_EN",""));row.put("MasterName_en",row.get("MasterName"));
  for(String key:List.of("UserNO","AnimalWeight","ChipNO","SampleArea","ReportNO","Remark"))row.put(key,"");
  for(String key:List.of("CityId","TestFee","DeliverySendGerFee","DeliverySendCNFee","InfoCheckReceivedId","SaveMedium","IsPayMoney","IsSendNoticeMail","CreateSampleId"))row.put(key,0);
  for(String key:List.of("ReceiveSampleDt","SendSampleDt","SendReportDt"))row.put(key,LocalDate.of(1900,1,1));
  return row;
 }
}
