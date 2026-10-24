package com.petlis.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 *
 * 用于生成和验证JWT token
 *
 * @author PETLIS Team
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private Long expiration;

    /**
     * 生成JWT token
     *
     * @param userId 用户ID
     * @param openid 微信openid
     * @param hospitalId 医院ID
     * @param cityId 城市ID
     * @return JWT token
     */
    public String generateToken(Integer userId, String openid, Integer hospitalId, Integer cityId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("openid", openid)
                .claim("hospitalId", hospitalId)
                .claim("cityId", cityId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从token中解析用户ID
     *
     * @param token JWT token
     * @return 用户ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Integer.valueOf(claims.getSubject());
    }

    /**
     * 从token中解析openid
     *
     * @param token JWT token
     * @return 微信openid
     */
    public String getOpenidFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("openid", String.class);
    }

    /**
     * 从token中解析医院ID
     *
     * @param token JWT token
     * @return 医院ID
     */
    public Integer getHospitalIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("hospitalId", Integer.class);
    }

    /**
     * 从token中解析城市ID
     *
     * @param token JWT token
     * @return 城市ID
     */
    public Integer getCityIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("cityId", Integer.class);
    }

    /**
     * 验证token
     *
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析token
     *
     * @param token JWT token
     * @return Claims对象
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取签名密钥
     *
     * @return 密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
