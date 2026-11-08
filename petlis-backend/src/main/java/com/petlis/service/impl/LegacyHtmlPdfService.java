package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service @RequiredArgsConstructor
public class LegacyHtmlPdfService {
 private final FeatureStore store;private final AccessService access;private final LegacyHtmlPdfRenderer renderer;
 public record PreviewCommand(String html){}
 public Map<String,Boolean> capabilities(){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-html-pdf' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");if(rows.size()!=1)return Map.of("htmlPdf",false);try{var enabled=new ObjectMapper().readTree(rows.get(0).get("config_value").toString()).path("enabled");return Map.of("htmlPdf",enabled.isBoolean()&&enabled.asBoolean());}catch(Exception e){throw new BusinessException(409,"HTML报告PDF参数无效");}}
 public byte[] preview(PreviewCommand command){if(!capabilities().get("htmlPdf"))throw new BusinessException(409,"HTML报告PDF预览尚未开启");if(command==null)throw new BusinessException(400,"PDF正文缺失");return renderer.render(command.html());}
}
