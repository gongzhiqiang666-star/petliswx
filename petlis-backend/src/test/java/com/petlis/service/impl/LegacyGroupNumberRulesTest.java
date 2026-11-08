package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LegacyGroupNumberRulesTest {
 @Test void prefixUsesLatestNumberAndFiveBytePadding(){assertEquals("P-00010",LegacyGroupNumberRules.preview("P-","P-00009",3));assertEquals("P-00001",LegacyGroupNumberRules.preview("P-",null,0));assertEquals("P-100000",LegacyGroupNumberRules.preview("P-","P-99999",1));}
 @Test void noPrefixRetainsPositiveLatestRatherThanInventingIncrement(){assertEquals("00009",LegacyGroupNumberRules.preview("","00009",100));assertEquals("00004",LegacyGroupNumberRules.preview("",null,3));}
 @Test void malformedZeroAndNegativeUseCountFallback(){assertEquals("P-00008",LegacyGroupNumberRules.preview("P-","P-ABC",7));assertEquals("P-00008",LegacyGroupNumberRules.preview("P-","P--1",7));assertEquals("00008",LegacyGroupNumberRules.preview("","0",7));}
 @Test void originalReplaceIsGlobalAndPhpAlphanumericIncrementIsRetained(){assertEquals("P-00013",LegacyGroupNumberRules.preview("P-","P-12P-",4));assertEquals("P-0012b",LegacyGroupNumberRules.preview("P-","P-12a",4));assertEquals("P-0010A",LegacyGroupNumberRules.preview("P-","P-9Z",4));assertEquals("P-00101",LegacyGroupNumberRules.preview("P-","P-1e2",4));}
}
