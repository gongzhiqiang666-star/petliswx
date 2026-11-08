package com.petlis.integration.sf;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class SfSandboxConfigurationTest {
 @TempDir Path dir;
 @Test void cannotUseProductionEndpointEvenWithValidLookingCredentials() throws Exception {
  Path file=dir.resolve("config.json");
  Files.writeString(file,"{\"enabled\":true,\"partnerId\":\"fixture\",\"checkword\":\"fixture\",\"endpoint\":\"https://sfapi.sf-express.com/std/service\"}");
  var config=new SfSandboxConfiguration(new ObjectMapper(),file.toString());
  assertEquals(503,assertThrows(BusinessException.class,config::load).getCode());
 }
 @Test void absentConfigurationStaysDisabled() {
  assertThrows(BusinessException.class,()->new SfSandboxConfiguration(new ObjectMapper(),"").load());
 }
}
