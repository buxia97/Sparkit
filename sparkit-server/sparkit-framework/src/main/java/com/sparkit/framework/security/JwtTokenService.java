package com.sparkit.framework.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 工具类
 */
@Slf4j
@Component
public class JwtTokenService {

    @Value("${sparkit.jwt.secret:Sparkit@2024!JWTSecretKeyForTokenGenerationMustBeLongEnough}")
    private String secret;

    @Value("${sparkit.jwt.access-expire:900}")
    private long accessExpire;

    @Value("${sparkit.jwt.refresh-expire:604800}")
    private long refreshExpire;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 AccessToken
     */
    public String generateAccessToken(Long userId, String userType, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userType", userType);
        claims.put("username", username);
        claims.put("type", "access");
        return createToken(claims, accessExpire);
    }

    /**
     * 生成 RefreshToken
     */
    public String generateRefreshToken(Long userId, String userType, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userType", userType);
        claims.put("username", username);
        claims.put("type", "refresh");
        return createToken(claims, refreshExpire);
    }

    private String createToken(Map<String, Object> claims, long expireSeconds) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000))
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (JwtException e) {
            log.error("JWT parse error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        Claims claims = parseToken(token);
        return claims != null && !claims.getExpiration().before(new Date());
    }

    /**
     * 从 Token 获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }

    /**
     * 从 Token 获取用户类型
     */
    public String getUserType(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userType", String.class) : null;
    }

    /**
     * 从 Token 获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }
}