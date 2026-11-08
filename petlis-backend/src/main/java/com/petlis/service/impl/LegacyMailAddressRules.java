package com.petlis.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Source: common/library/Email.php::buildAddress and finance/Hospmonthpeport.php.
 * Parses the legacy string contract only; transport must still validate addresses.
 * Empty or malformed source values are retained, never silently discarded.
 */
public final class LegacyMailAddressRules {
    private LegacyMailAddressRules() { }

    public record Address(String email, String name) { }

    public static List<Address> parse(String source) {
        rejectHeaderInjection(source);
        var addresses = new LinkedHashMap<String, Address>();
        for (String email : source.replace(';', ',').split(",", -1)) {
            int at = email.indexOf('@');
            addresses.put(email, new Address(email, at < 0 ? "" : email.substring(0, at)));
        }
        return List.copyOf(addresses.values());
    }

    public static String senderName(String username) {
        rejectHeaderInjection(username);
        return username.replace("@laboklin.cn", "");
    }

    private static void rejectHeaderInjection(String value) {
        Objects.requireNonNull(value, "Mail header value is required");
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Mail headers cannot contain control delimiters");
        }
    }
}
