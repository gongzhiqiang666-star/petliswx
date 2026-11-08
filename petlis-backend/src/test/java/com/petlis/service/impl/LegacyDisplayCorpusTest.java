package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
/** Private transient corpus only. Never put report text in assertions or Surefire output. */
@EnabledIfEnvironmentVariable(named="PETLIS_DISPLAY_CORPUS",matches=".+")
class LegacyDisplayCorpusTest {
 @Test void everyOriginalPhpDisplayResultMatchesJava()throws Exception{
  Path root=Path.of(System.getProperty("user.home"),".local/share/petlis/qa").toRealPath();
  Path input=Path.of(System.getenv("PETLIS_DISPLAY_CORPUS")).toRealPath();assertEquals(root,input.getParent());
  assertEquals(PosixFilePermissions.fromString("rw-------"),Files.getPosixFilePermissions(input));
  var mapper=new ObjectMapper();var corpus=mapper.readTree(Files.readString(input));var rows=corpus.path("rows");
  assertTrue(rows.isArray());assertTrue(rows.size()>0);assertEquals(corpus.path("count").asInt(),rows.size());
  int index=0;for(var row:rows){assertTrue(row.path("raw").isTextual());assertTrue(row.path("expected").isTextual());
   assertTrue(row.path("expected").asText().equals(LegacyDictionarySelector.display(row.path("raw").asText())),"Original PHP display mismatch at corpus index "+index);index++;
  }
  var proof=Map.of("nonce",corpus.path("nonce").asText(),"count",index,"matched",true,"sourceHash",corpus.path("sourceHash").asText());
  Path result=Path.of(input+".proof.json");Files.createFile(result,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
  Files.writeString(result,mapper.writeValueAsString(proof));
 }
}
