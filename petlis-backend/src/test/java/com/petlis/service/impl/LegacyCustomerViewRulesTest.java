package com.petlis.service.impl;
import org.junit.jupiter.api.Test;import java.time.*;import static org.junit.jupiter.api.Assertions.*;
class LegacyCustomerViewRulesTest {
 @Test void twentiethDaySwitchAndPhpMonthOverflowArePreserved(){assertEquals(LocalDateTime.of(2024,1,1,0,0),LegacyCustomerViewRules.debtCutoff(LocalDate.of(2024,3,19)));assertEquals(LocalDateTime.of(2024,2,1,0,0),LegacyCustomerViewRules.debtCutoff(LocalDate.of(2024,3,20)));assertEquals(LocalDateTime.of(2024,3,1,0,0),LegacyCustomerViewRules.debtCutoff(LocalDate.of(2024,3,31)));}
}
