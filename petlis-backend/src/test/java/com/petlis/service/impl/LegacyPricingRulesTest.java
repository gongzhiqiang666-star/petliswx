package com.petlis.service.impl;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyPricingRulesTest {
 @Test void originalPhpReferenceCasesMatchTypesAndDisplayExactly()throws Exception{
  Path p=Path.of("../docs/php-parity/pricing-reference.json");if(!Files.exists(p))p=Path.of("docs/php-parity/pricing-reference.json");
  var mapper=new ObjectMapper();var rows=mapper.readTree(Files.readString(p)).path("rows");assertTrue(rows.size()>=223);
  for(var row:rows){Object value=mapper.convertValue(row.get("input"),Object.class);
   if(row.has("originalDiscount")){assertEquals(row.get("originalDiscount"),mapper.valueToTree(LegacyPricingRules.originalDiscount(value)),row.toString());assertEquals(row.path("discountToInt").asLong(),LegacyPricingRules.discountToInt(value),row.toString());}
   else assertEquals(row.path("showPrice").asText(),LegacyPricingRules.display(String.valueOf(value)),row.toString());
  }
 }
 @Test void quoteTruncatesFactorBeforeMultiplicationAndThenFormatsLikePhp(){
  assertEquals("75.00",LegacyPricingRules.quote("100.00","7.55"));assertEquals("1.25",LegacyPricingRules.quote("12.59","1"));assertEquals("-12.55",LegacyPricingRules.quote("125.50","-1"));assertEquals("0.00",LegacyPricingRules.quote("100.00","0.09"));
 }
 @Test void originalAndCustomQuoteModesDoNotNormalizeDifferentChineseLabels(){
  assertEquals("125.50",LegacyPricingRules.quote("125.50","原价"));assertEquals("125.50",LegacyPricingRules.quote("125.50","10.00"));assertEquals("125.50",LegacyPricingRules.quote("125.50","其他折扣"));assertEquals(-1,LegacyPricingRules.originalDiscount("其它折扣"));assertEquals("其他折扣",LegacyPricingRules.originalDiscount("其他折扣"));
 }
 @Test void invalidInputIsControlledInsteadOfProducingNanOrImplicitZero(){assertThrows(RuntimeException.class,()->LegacyPricingRules.quote("NaN","9"));assertThrows(RuntimeException.class,()->LegacyPricingRules.quote("10",null));assertThrows(RuntimeException.class,()->LegacyPricingRules.display("1".repeat(300)));}
 @Test void originalBcmathQuoteAndHospitalReferencesMatch()throws Exception{
  Path p=Path.of("../docs/php-parity/bcmath-reference.json");if(!Files.exists(p))p=Path.of("docs/php-parity/bcmath-reference.json");
  var reference=new ObjectMapper().readTree(Files.readString(p));assertTrue(reference.path("runtime").path("bcmath").asBoolean());int quotes=0,hospital=0;
  for(var row:reference.path("rows")){
   if("quote".equals(row.path("kind").asText())){quotes++;var in=row.path("input");assertEquals(row.path("output").asText(),LegacyPricingRules.quote(in.path("price").asText(),in.path("discount").asText()),row.toString());}
   else {hospital++;var before=new java.math.BigDecimal(row.path("input").get(0).asText());var delta=new java.math.BigDecimal(row.path("input").get(1).asText());var expected=new java.math.BigDecimal(row.path("output").asText());if(delta.signum()<0&&expected.signum()<0)assertThrows(com.petlis.common.exception.BusinessException.class,()->LegacyHospitalFundsRules.balanceAfter(before,delta,"A"));else assertEquals(expected,LegacyHospitalFundsRules.balanceAfter(before,delta,"A"),row.toString());}
  }
  assertEquals(100,quotes);assertEquals(7,hospital);
 }
}
