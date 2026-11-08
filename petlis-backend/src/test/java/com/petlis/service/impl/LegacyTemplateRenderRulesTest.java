package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LegacyTemplateRenderRulesTest {
 @SuppressWarnings("unchecked") @Test void originalBodyReferenceCasesMatchExactly()throws Exception{
  Path p=Path.of("../docs/php-parity/render-reference.json");if(!Files.exists(p))p=Path.of("docs/php-parity/render-reference.json");var mapper=new ObjectMapper();var rows=mapper.readTree(Files.readString(p)).path("rows");assertEquals(40,rows.size());
  for(var reference:rows){var input=reference.path("input");var bindings=new ArrayList<LegacyTemplateRules.Binding>();for(var b:input.path("bindings"))bindings.add(new LegacyTemplateRules.Binding(b.path("table").asText(),b.path("column").asText(),b.path("description").asText()));
   Map<String,Map<String,Object>> data=mapper.convertValue(input.path("rows"),Map.class);String body=input.get("body").isNull()?null:input.path("body").asText();String names=input.get("diagnosticNames").isNull()?null:input.path("diagnosticNames").asText();
   assertEquals(reference.path("output").asText(),LegacyTemplateRules.render(body,bindings,data,input.path("url").asText(),LocalDate.parse(input.path("today").asText()),names,input.path("type").asInt()),input.toString());
  }
 }
}
