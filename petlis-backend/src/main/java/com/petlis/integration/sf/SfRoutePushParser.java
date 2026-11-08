package com.petlis.integration.sf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.petlis.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;

/** RoutePushService JSON decoder only. Parsing does not authenticate or authorize a callback. */
public final class SfRoutePushParser {
    private static final int MAX_BYTES=256*1024;
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private final ObjectMapper mapper;
    public SfRoutePushParser(ObjectMapper mapper) {
        this.mapper=mapper.copy().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.mapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(12).maxStringLength(4096).build());
    }
    public record Route(String trackingNo,String externalOrderId,ObjectNode event) { }
    public List<Route> parse(String json) {
        if(json==null || json.length()>MAX_BYTES || json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES) throw invalid();
        try {
            JsonNode root=mapper.readTree(json);
            if(root==null || !root.isObject() || !root.path("Body").isObject()) throw invalid();
            JsonNode nodes=root.path("Body").path("WaybillRoute");
            if(!nodes.isArray() || nodes.isEmpty() || nodes.size()>10) throw invalid();
            var result=new ArrayList<Route>();
            for(JsonNode node:nodes) {
                if(!node.isObject()) throw invalid();
                String mail=text(node,"mailno",64,true),order=text(node,"orderid",64,true),time=text(node,"acceptTime",19,true),code=text(node,"opCode",32,true);
                if(!mail.matches("[A-Za-z0-9-]{6,64}")) throw invalid();
                LocalDateTime.parse(time,TIME);
                ObjectNode event=mapper.createObjectNode();event.put("acceptTime",time);event.put("opCode",code);
                event.put("acceptAddress",text(node,"acceptAddress",250,false));event.put("remark",text(node,"remark",1000,false));
                event.put("firstStatusCode",text(node,"firstStatusCode",32,false));
                event.put("secondaryStatusCode",text(node,"secondaryStatusCode",32,false));
                result.add(new Route(mail,order,event));
            }
            return List.copyOf(result);
        } catch(BusinessException error) { throw error; }
        catch(Exception error) { throw invalid(); }
    }
    private String text(JsonNode node,String key,int max,boolean required) {
        JsonNode value=node.get(key);
        if(value==null || value.isNull()) { if(required) throw invalid();return ""; }
        if(!value.isTextual()) throw invalid();
        String text=value.textValue();
        if(text.length()>max || text.chars().anyMatch(c->Character.isISOControl(c)) || (required&&text.isBlank())) throw invalid();
        return text;
    }
    private BusinessException invalid(){return new BusinessException(400,"顺丰路由推送JSON格式无效");}
}
