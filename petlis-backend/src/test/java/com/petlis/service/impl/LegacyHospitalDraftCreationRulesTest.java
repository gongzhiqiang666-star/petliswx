package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class LegacyHospitalDraftCreationRulesTest {
 @Test void monthEndAndLeapDayOverflowAreNotClamped(){
  assertEquals(LocalDate.of(2024,3,2),LegacyHospitalDraftCreationRules.birthday("0岁1月",LocalDate.of(2024,3,31)));
  assertEquals(LocalDate.of(2023,3,1),LegacyHospitalDraftCreationRules.birthday("1岁0月",LocalDate.of(2024,2,29)));
  assertEquals(LocalDate.of(2022,11,9),LegacyHospitalDraftCreationRules.birthday("2岁12月",LocalDate.of(2025,11,9)));
 }
 @Test void originalEmptyAndPlaceholderAgeHaveZeroOffsets(){var day=LocalDate.of(2026,11,9);assertEquals(day,LegacyHospitalDraftCreationRules.birthday(" - 岁 - 月",day));assertEquals(day,LegacyHospitalDraftCreationRules.birthday("",day));}
 @Test void modelCopiesSelectedBreedAndOverwritesCreationOnlyFields(){
  var r=LegacyHospitalDraftCreationRules.defaults(Map.of("MasterName","主人","AnimalName","动物","Age","2岁3月","SampleArea","部位","Remark","备注"),7,LocalDate.of(2026,11,9),Map.of("Variety_CN","品种中文","Variety_EN","Breed"));
  assertEquals(LocalDate.of(2024,8,9),r.get("Birthday"));assertEquals("主人",r.get("MasterName_en"));assertEquals("品种中文",r.get("AnimalType"));assertEquals("Breed",r.get("AnimalType_en"));assertEquals("",r.get("SampleArea"));assertEquals("",r.get("Remark"));assertEquals(-2,r.get("SampleStatus"));assertEquals(7,r.get("InfoHospitalId"));assertEquals(2,r.get("CreateDataTypeIId"));
 }
}
