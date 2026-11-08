package com.petlis.service.impl;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMailAddressRulesTest {
    @Test void sourceSeparatorsDuplicatesAndNamesArePreserved() {
        var addresses = LegacyMailAddressRules.parse("a@example.test;b@example.test,a@example.test");
        assertEquals(List.of("a@example.test", "b@example.test"), addresses.stream().map(LegacyMailAddressRules.Address::email).toList());
        assertEquals(List.of("a", "b"), addresses.stream().map(LegacyMailAddressRules.Address::name).toList());
    }

    @Test void noSilentTrimmingOrDroppingSourceValues() {
        var addresses = LegacyMailAddressRules.parse(" a@example.test,,invalid");
        assertEquals(" a@example.test", addresses.get(0).email());
        assertEquals(" a", addresses.get(0).name());
        assertEquals("", addresses.get(1).email());
        assertEquals("", addresses.get(2).name());
    }

    @Test void displayNameUsesLiteralCaseSensitiveReplacement() {
        assertEquals("sender", LegacyMailAddressRules.senderName("sender@laboklin.cn"));
        assertEquals("sender@LABOKLIN.CN", LegacyMailAddressRules.senderName("sender@LABOKLIN.CN"));
        assertEquals("sender.other", LegacyMailAddressRules.senderName("sender@laboklin.cn.other"));
    }

    @Test void headerInjectionIsRejectedRatherThanSilentlyChanged() {
        assertThrows(IllegalArgumentException.class, () -> LegacyMailAddressRules.parse("a@example.test\r\nBcc:b@example.test"));
        assertThrows(IllegalArgumentException.class, () -> LegacyMailAddressRules.senderName("a\n@example.test"));
    }
}
