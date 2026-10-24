package com.petlis.common.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信工具类
 *
 * 用于微信小程序登录
 *
 * @author PETLIS Team
 */
@Slf4j
@Component
public class WechatUtil {

    @Value("${app.wechat.appid}")
    private String appid;

    @Value("${app.wechat.secret}")
    private String secret;

    @Value("${app.wechat.url}")
    private String url;

    /**
     * 通过code换取openid
     *
     * @param code 微信登录code
     * @return openid和session_key
     */
    public WechatLoginResponse code2Session(String code) {
        String requestUrl = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                url, appid, secret, code);

        try {
            String response = HttpUtil.get(requestUrl);
            log.info("Wechat code2Session response: {}", response);

            JSONObject jsonObject = JSONUtil.parseObj(response);

            // 检查是否有错误
            if (jsonObject.containsKey("errcode")) {
                Integer errcode = jsonObject.getInt("errcode");
                String errmsg = jsonObject.getStr("errmsg");
                throw new RuntimeException("Wechat login failed: " + errmsg + " (errcode: " + errcode + ")");
            }

            // 解析成功响应
            String openid = jsonObject.getStr("openid");
            String sessionKey = jsonObject.getStr("session_key");

            return new WechatLoginResponse(openid, sessionKey);
        } catch (Exception e) {
            log.error("Wechat code2Session error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get openid from Wechat: " + e.getMessage(), e);
        }
    }

    /**
     * 微信登录响应
     */
    public record WechatLoginResponse(String openid, String sessionKey) {
    }
}
