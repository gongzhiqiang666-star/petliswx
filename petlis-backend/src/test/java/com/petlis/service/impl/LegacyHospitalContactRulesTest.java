package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LegacyHospitalContactRulesTest {
 @Test void retainsOriginalContentAndFinanceFlag(){var c=LegacyHospitalContactRules.validate(new LegacyHospitalContactRules.Contact(" 姓名 ","电话","a@example.test","传真","备注",1));assertEquals(" 姓名 ",c.name());assertEquals(1,c.financeContact());}
 @Test void mailRequiredButNoStricterFormatInvented(){assertThrows(RuntimeException.class,()->LegacyHospitalContactRules.validate(new LegacyHospitalContactRules.Contact("","","","","",0)));assertEquals("历史邮箱文本",LegacyHospitalContactRules.validate(new LegacyHospitalContactRules.Contact(null,null,"历史邮箱文本",null,null,0)).mail());}
 @Test void limitsUseUnicodeCharactersAndOriginalColumnLengths(){assertDoesNotThrow(()->LegacyHospitalContactRules.validate(new LegacyHospitalContactRules.Contact("猫".repeat(250),"","mail","","",0)));assertThrows(RuntimeException.class,()->LegacyHospitalContactRules.validate(new LegacyHospitalContactRules.Contact("猫".repeat(251),"","mail","","",0)));}
}
