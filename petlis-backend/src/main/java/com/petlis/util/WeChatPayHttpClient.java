package com.petlis.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信支付HTTP客户端
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Component
public class WeChatPayHttpClient {

    /**
     * 发送POST请求到微信支付API
     *
     * @param url 请求URL
     * @param xmlData XML格式的请求数据
     * @return 响应的XML数据
     */
    public String postXml(String url, String xmlData) {
        HttpURLConnection conn = null;
        OutputStream out = null;
        BufferedReader reader = null;
        
        try {
            // 创建连接
            URL apiUrl = new URL(url);
            conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/xml");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // 发送数据
            out = conn.getOutputStream();
            out.write(xmlData.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // 读取响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            } else {
                log.error("微信支付API调用失败, 响应码: {}", responseCode);
                throw new RuntimeException("微信支付API调用失败, 响应码: " + responseCode);
            }
        } catch (Exception e) {
            log.error("调用微信支付API异常", e);
            throw new RuntimeException("调用微信支付API异常: " + e.getMessage(), e);
        } finally {
            try {
                if (out != null) out.close();
                if (reader != null) reader.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
                log.error("关闭连接失败", e);
            }
        }
    }

    /**
     * Map转XML
     */
    public String mapToXml(Map<String, String> params) {
        StringBuilder xml = new StringBuilder();
        xml.append("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if(!entry.getKey().matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("Invalid XML field");
            xml.append("<").append(entry.getKey()).append(">")
               .append("<![CDATA[").append(entry.getValue()==null?"":entry.getValue().replace("]]>","]]]]><![CDATA[>")).append("]]>")
               .append("</").append(entry.getKey()).append(">");
        }
        xml.append("</xml>");
        return xml.toString();
    }

    /**
     * XML转Map
     */
    public Map<String, String> xmlToMap(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(
                    new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            if (!"xml".equals(doc.getDocumentElement().getTagName())) throw new IllegalArgumentException("Invalid root");
            Map<String, String> result = new java.util.HashMap<>();
            org.w3c.dom.NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Node node = nodes.item(i);
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                if (result.putIfAbsent(node.getNodeName(), node.getTextContent()) != null) {
                    throw new IllegalArgumentException("Duplicate XML field");
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("支付 XML 无效", e);
        }
    }
}
