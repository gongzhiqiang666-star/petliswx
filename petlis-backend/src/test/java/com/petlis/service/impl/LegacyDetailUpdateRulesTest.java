package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class LegacyDetailUpdateRulesTest {
 private final LocalDateTime now=LocalDateTime.of(2026,11,8,10,11,12);
 @Test void emptyDateAndUnsentPreservesParentAndUses1970Sentinel(){var effect=LegacyDetailUpdateRules.effects(0,"",now);assertFalse(effect.completeParent());assertEquals(LocalDateTime.of(1970,1,1,0,0),effect.sentAt());assertEquals("2026.11.08,宠物,主人,U1,R结果.pdf",LegacyDetailUpdateRules.filename(project(),effect,now));}
 @Test void sentWithOldDateUsesNowAndCompletesParent(){var effect=LegacyDetailUpdateRules.effects(1,"1900-01-01",now);assertTrue(effect.completeParent());assertEquals(now,effect.sentAt());}
 @Test void nonemptyDateCompletesParentEvenWhenUnsentAsOriginalCallback(){var effect=LegacyDetailUpdateRules.effects(0,"2024-05-06",now);assertTrue(effect.completeParent());assertEquals("2024.05.06,宠物,主人,U1,R结果.pdf",LegacyDetailUpdateRules.filename(project(),effect,now));}
 @Test void filenameUsesOriginalQuoteSpaceAmpersandHashRules(){var project=Map.<String,Object>of("AnimalName"," A & B# ","MasterName","'主人'","UserNO"," U1 ","ReportNO","R","DiagnosticResults","\"结论\"");assertEquals("2024.05.06,A和B,主人,U1,R结论.pdf",LegacyDetailUpdateRules.filename(project,LegacyDetailUpdateRules.effects(0,"2024-05-06",now),now));}
 private Map<String,Object> project(){return Map.of("AnimalName"," 宠物 ","MasterName"," 主人 ","UserNO"," U1 ","ReportNO","R","DiagnosticResults","结果");}
}
