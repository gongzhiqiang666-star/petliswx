package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyTemplateRulesTest {
 @SuppressWarnings("unchecked") @Test void originalTitleReferenceKeepsWhitespaceEmptyRulesAndCommaOnlyTrim()throws Exception{
  Path p=Path.of("../docs/php-parity/title-reference.json");if(!Files.exists(p))p=Path.of("docs/php-parity/title-reference.json");var mapper=new ObjectMapper();var rows=mapper.readTree(Files.readString(p)).path("rows");assertEquals(8,rows.size());for(var row:rows){Map<String,Object> project=mapper.convertValue(row.path("input").path("project"),Map.class);var override=row.path("input").get("override");assertEquals(row.path("output").asText(),LegacyTemplateRules.mailTitle(project,override.isNull()?null:override.asText()),row.toString());}
 }
}
