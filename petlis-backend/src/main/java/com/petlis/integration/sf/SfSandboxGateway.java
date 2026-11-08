package com.petlis.integration.sf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/** Sandbox-only protocol gateway. Never retries shipment creation automatically. */
public final class SfSandboxGateway {
    public static final URI ENDPOINT=URI.create("https://sfapi-sbox.sf-express.com/std/service");
    public interface Transport { String post(String form) throws Exception; }
    public record Outcome(String platformCode, String businessCode, boolean success, JsonNode data) {}
    private final String partnerId, checkword;
    private final ObjectMapper mapper;
    private final Transport transport;
    public SfSandboxGateway(String partnerId,String checkword,ObjectMapper mapper,Transport transport) {
        if(partnerId==null||partnerId.isBlank()||checkword==null||checkword.isBlank())throw new IllegalArgumentException("顺丰沙箱未配置");
        this.partnerId=partnerId;this.checkword=checkword;this.mapper=mapper;this.transport=transport;
    }
    public static Transport httpTransport() {
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
        return form -> {
            var request=HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(20))
                .header("Content-Type","application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new IllegalStateException("顺丰沙箱 HTTP "+response.statusCode()+"，请先查询订单结果");
            return response.body();
        };
    }
    public Outcome call(String serviceCode,Map<String,Object> payload,String requestId,long timestamp) throws Exception {
        if(!Set.of("EXP_RECE_CREATE_ORDER","EXP_RECE_SEARCH_ORDER_RESP","EXP_RECE_UPDATE_ORDER","EXP_RECE_SEARCH_ROUTES","EXP_RECE_REGISTER_ROUTE","COM_RECE_CLOUD_PRINT_WAYBILLS").contains(serviceCode))
            throw new IllegalArgumentException("未批准的顺丰服务");
        UUID.fromString(requestId);
        String msg=mapper.writeValueAsString(payload);
        String digest=Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest((msg+timestamp+checkword).getBytes(StandardCharsets.UTF_8)));
        var params=new LinkedHashMap<String,String>();
        params.put("partnerID",partnerId);params.put("requestID",requestId);params.put("serviceCode",serviceCode);
        params.put("timestamp",Long.toString(timestamp));params.put("msgDigest",digest);params.put("msgData",msg);
        String form=params.entrySet().stream().map(e->encode(e.getKey())+"="+encode(e.getValue())).collect(java.util.stream.Collectors.joining("&"));
        JsonNode envelope=mapper.readTree(transport.post(form));
        String code=envelope.path("apiResultCode").asText();
        JsonNode business=envelope.path("apiResultData");
        if(business.isTextual())business=mapper.readTree(business.asText());
        if("COM_RECE_CLOUD_PRINT_WAYBILLS".equals(serviceCode)) {
            JsonNode obj=business.path("obj");
            boolean printed="A1000".equals(code) && business.path("success").asBoolean(false)
                && "pdf".equals(obj.path("fileType").asText()) && obj.path("files").isArray() && !obj.path("files").isEmpty();
            return new Outcome(code,business.path("errorCode").asText(),printed,printed?obj:mapper.nullNode());
        }
        boolean success="A1000".equals(code)&&business.path("success").asBoolean(false)&&"S0000".equals(business.path("errorCode").asText());
        JsonNode data=business.path("msgData");
        if(data.isTextual())data=mapper.readTree(data.asText());
        // Update responses may be platform/business-success while order numbers mismatch.
        if("EXP_RECE_UPDATE_ORDER".equals(serviceCode))
            success=success && data.path("resStatus").asInt(-1)==2;
        return new Outcome(code,business.path("errorCode").asText(),success,success?data:mapper.nullNode());
    }
    private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
