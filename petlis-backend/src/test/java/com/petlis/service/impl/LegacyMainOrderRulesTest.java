package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyMainOrderRulesTest {
 @Test void adminDefaultsMatchOriginalOverridesWithoutMutatingInput(){var input=new LinkedHashMap<String,Object>(Map.of("InfoHospitalId",99,"AnimalName","old","TestFee","100.00","SampleStatus",2));var out=LegacyMainOrderRules.adminInsert(input);assertEquals(0,out.get("InfoHospitalId"));assertEquals("",out.get("AnimalName"));assertEquals(0,out.get("TestFee"));assertEquals("55550-",out.get("UserNO"));assertEquals("1900-01-01",out.get("ReceiveSampleDt"));assertEquals(2,out.get("SampleStatus"));assertEquals("old",input.get("AnimalName"));}
 @Test void sexDefaultUsesOriginalSampleConditionNotAnInventedSexCondition(){var out=LegacyMainOrderRules.adminInsert(Map.of("InfoSampleTypeId",3,"DictSexId",7));assertEquals(7,out.get("DictSexId"));out=LegacyMainOrderRules.adminInsert(Map.of("InfoSampleTypeId",0,"DictSexId",7));assertEquals(0,out.get("DictSexId"));}
 @Test void updateEmptyDatesAreOmittedButWhitespaceAndSentinelsPreserved(){var input=new LinkedHashMap<String,Object>();input.put("Birthday",null);input.put("CollectDt","");input.put("SendSampleDt","0");input.put("ReceiveSampleDt","1900-01-01");input.put("SendReportDt"," ");input.put("AnimalName","new");var out=LegacyMainOrderRules.update(input);assertFalse(out.containsKey("Birthday"));assertFalse(out.containsKey("CollectDt"));assertFalse(out.containsKey("SendSampleDt"));assertEquals("1900-01-01",out.get("ReceiveSampleDt"));assertEquals(" ",out.get("SendReportDt"));assertEquals("new",out.get("AnimalName"));assertTrue(input.containsKey("Birthday"));}
}
