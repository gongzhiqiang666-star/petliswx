package com.petlis.service.impl;
import java.time.*;
/** Project/View.Detail: PHP relative month arithmetic carries overflowing days, it does not clamp. */
final class LegacyCustomerViewRules {
 private LegacyCustomerViewRules(){}
 static LocalDateTime debtCutoff(LocalDate today){int months=today.getDayOfMonth()>=20?1:2;return today.withDayOfMonth(1).minusMonths(months).plusDays(today.getDayOfMonth()-1L).withDayOfMonth(1).atStartOfDay();}
}
