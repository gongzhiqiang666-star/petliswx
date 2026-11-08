package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LegacyMailRoutingRulesTest {
 @Test void reportOverrideReplacesBothToAndBcc(){var route=LegacyMailRoutingRules.route(LegacyMailRoutingRules.Kind.REPORT," original@example.test ","sandbox@example.test","audit@example.test");assertEquals("sandbox@example.test",route.to());assertEquals("sandbox@example.test",route.bcc());}
 @Test void financeAndSampleOverrideOnlyTo(){for(var kind:new LegacyMailRoutingRules.Kind[]{LegacyMailRoutingRules.Kind.FINANCE,LegacyMailRoutingRules.Kind.SAMPLE_NOTICE}){var route=LegacyMailRoutingRules.route(kind,"original@example.test","sandbox@example.test","audit@example.test");assertEquals("sandbox@example.test",route.to());assertEquals("audit@example.test",route.bcc());}}
 @Test void onlyReportAppliesOriginalPhpTrimAndPhpZeroDoesNotOverride(){for(var kind:LegacyMailRoutingRules.Kind.values()){var route=LegacyMailRoutingRules.route(kind," \tmail@example.test\r\n","0","audit@example.test");assertEquals(kind==LegacyMailRoutingRules.Kind.REPORT?"mail@example.test":" \tmail@example.test\r\n",route.to());}assertEquals("\fmail@example.test\f",LegacyMailRoutingRules.route(LegacyMailRoutingRules.Kind.REPORT,"\fmail@example.test\f",null,"bcc").to());}
}
