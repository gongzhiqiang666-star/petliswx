package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LegacyHtmlPdfServiceTest {
 @Test void strictFlagAndAdminProtectRenderer(){var store=mock(FeatureStore.class);var jdbc=mock(JdbcTemplate.class);var access=mock(AccessService.class);var renderer=mock(LegacyHtmlPdfRenderer.class);when(store.jdbc()).thenReturn(jdbc);var service=new LegacyHtmlPdfService(store,access,renderer);
  for(String config:List.of("{}","{\"enabled\":false}","{\"enabled\":\"true\"}")){when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of("config_value",config)));assertEquals(409,assertThrows(BusinessException.class,()->service.preview(new LegacyHtmlPdfService.PreviewCommand("<p>test</p>"))).getCode());}verifyNoInteractions(renderer);
  when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of("config_value","{\"enabled\":true}")));byte[] pdf="%PDF-test".getBytes();when(renderer.render("<p>test</p>")).thenReturn(pdf);assertSame(pdf,service.preview(new LegacyHtmlPdfService.PreviewCommand("<p>test</p>")));doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,service::capabilities).getCode());
 }
}
