package com.petlis.integration.sf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SfSandboxConfiguration {
    private final ObjectMapper mapper;
    private final String path;
    public SfSandboxConfiguration(ObjectMapper mapper,@Value("${app.sf.sandbox-config:}") String path) {this.mapper=mapper;this.path=path;}
    public record Settings(SfSandboxGateway gateway,String templateCode) {}
    public Settings load() {
        try {
            if(path.isBlank()) throw new IllegalStateException();
            var config=mapper.readTree(Files.readString(Path.of(path)));
            if(!config.path("enabled").asBoolean(false) || !SfSandboxGateway.ENDPOINT.toString().equals(config.path("endpoint").asText())) throw new IllegalStateException();
            String partner=config.path("partnerId").asText();
            var gateway=new SfSandboxGateway(partner,config.path("checkword").asText(),mapper,SfSandboxGateway.httpTransport());
            return new Settings(gateway,"fm_76130_standard_"+partner);
        } catch(Exception e) {throw new BusinessException(503,"顺丰沙箱未配置或未启用");}
    }
}
