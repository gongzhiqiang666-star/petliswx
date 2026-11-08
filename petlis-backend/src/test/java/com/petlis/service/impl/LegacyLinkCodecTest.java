package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyLinkCodecTest {
 @Test void originalAlgorithmsMatchUsingPublicTestKeyOnly()throws Exception{Path p=Path.of("../docs/php-parity/link-reference.json");if(!Files.exists(p))p=Path.of("docs/php-parity/link-reference.json");var ref=new ObjectMapper().readTree(Files.readString(p));assertFalse(ref.path("productionKeyIncluded").asBoolean());String key=ref.path("testPhrase").asText();int count=0;for(var row:ref.path("rows")){count++;assertEquals(row.path("token").asText(),LegacyLinkCodec.encrypt(row.path("input").asText(),key));assertEquals(row.path("decoded").asText(),LegacyLinkCodec.decrypt(row.path("token").asText(),key),row.toString());}assertEquals(13,count);}
 @Test void invalidTokensDoNotBecomeAnAuthenticatedIdentifier(){assertEquals("",LegacyLinkCodec.decrypt("bad-token","petlis-public-test-key"));assertEquals("",LegacyLinkCodec.decrypt("","petlis-public-test-key"));}
}
