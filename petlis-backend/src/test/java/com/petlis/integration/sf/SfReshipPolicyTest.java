package com.petlis.integration.sf;
import org.junit.jupiter.api.Test;import org.springframework.jdbc.core.JdbcTemplate;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;
class SfReshipPolicyTest {
 SfReshipPolicy.Settings load(Map<String,Object> row){var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList(anyString())).thenReturn(row==null?List.of():List.of(row));return new SfReshipPolicy(jdbc).load(false);}
 @Test void missingIsDisabledAndValidBooleanUsesDefaultLimit(){var missing=load(null);assertFalse(missing.enabled());assertEquals(3,missing.maxAttempts());var enabled=load(Map.of("enabledType","BOOLEAN","enabled","true"));assertTrue(enabled.enabled());assertTrue(enabled.valid());assertEquals(3,enabled.maxAttempts());}
 @Test void invalidStringBooleanOrNonIntegerAndOutOfRangeLimitsFailClosed(){for(var row:List.of(Map.<String,Object>of("enabledType","STRING","enabled","true"),Map.<String,Object>of("enabledType","BOOLEAN","enabled","true","maxType","STRING","maxAttempts","3"),Map.<String,Object>of("enabledType","BOOLEAN","enabled","true","maxType","INTEGER","maxAttempts","11"),Map.<String,Object>of("enabledType","BOOLEAN","enabled","true","maxType","INTEGER","maxAttempts","1"))){var settings=load(row);assertFalse(settings.enabled());assertFalse(settings.valid());}}
}
