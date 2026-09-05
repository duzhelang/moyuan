package com.moyuan.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    // 令牌类型标识（用于区分 access / refresh，便于校验时不误用）
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpiration() {
        return expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * 生成访问令牌 access token（含 jti，用于登出失效黑名单）
     */
    public String generateToken(Long userId, String username) {
        return buildToken(userId, username, expiration, TOKEN_TYPE_ACCESS);
    }

    /**
     * 生成刷新令牌 refresh token（更长的有效期，用于换取新的访问令牌）
     */
    public String generateRefreshToken(Long userId, String username) {
        return buildToken(userId, username, refreshExpiration, TOKEN_TYPE_REFRESH);
    }

    private String buildToken(Long userId, String username, long ttlMillis, String type) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("username", username)
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** token 唯一标识 jti，用于登出失效黑名单 */
    public String getTokenId(String token) {
        return parseToken(token).getId();
    }

    /** token 类型（access / refresh） */
    public String getTokenType(String token) {
        Object type = parseToken(token).get("type");
        return type != null ? String.valueOf(type) : TOKEN_TYPE_ACCESS;
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 校验指定 token 是否是指定类型（防止用 refresh token 当 access token 使用） */
    public boolean isType(String token, String expectType) {
        try {
            return expectType.equals(getTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    /** 校验 access token 是否有效（可签、未过期、类型为 access） */
    public boolean validateAccessToken(String token) {
        return StringUtils.hasText(token) && validateToken(token) && isType(token, TOKEN_TYPE_ACCESS);
    }

    /** 校验 refresh token 是否有效（可签、未过期、类型为 refresh） */
    public boolean validateRefreshToken(String token) {
        return StringUtils.hasText(token) && validateToken(token) && isType(token, TOKEN_TYPE_REFRESH);
    }
}