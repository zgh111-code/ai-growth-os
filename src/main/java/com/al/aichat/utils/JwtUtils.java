package com.al.aichat.utils;

import com.al.aichat.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 *
 * 负责生成和验证 JWT 令牌。
 *
 * JWT 令牌结构：
 *   header.payload.signature
 *   （头部.载荷.签名）
 *
 * 例如：
 *   eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjE3MDA2MDUwMDB9.xxxxx
 *
 * @Component 让 Spring 管理这个类，这样我们可以注入 JwtConfig
 */
@Component
public class JwtUtils {

    private final JwtConfig jwtConfig;

    /**
     * 构造方法注入 JwtConfig
     */
    public JwtUtils(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * 生成 JWT 令牌
     *
     * @param userId   用户ID（存在令牌中，后续可以从令牌中取出）
     * @param username 用户名（存在令牌中，方便识别）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        // 1. 将字符串密钥转为 SecretKey 对象（JJWT 要求的格式）
        SecretKey key = Keys.hmacShaKeyFor(
                jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)
        );

        // 2. 计算过期时间
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration());

        // 3. 构建 JWT
        return Jwts.builder()
                // 设置主题（这里存用户ID）
                .subject(String.valueOf(userId))
                // 自定义声明（存用户名）
                .claim("username", username)
                // 签发时间
                .issuedAt(now)
                // 过期时间
                .expiration(expiration)
                // 签名算法和密钥
                .signWith(key)
                // 生成紧凑格式的字符串
                .compact();
    }

    /**
     * 从 JWT 令牌中解析出用户ID
     *
     * @param token JWT 字符串
     * @return 用户ID，如果令牌无效则返回 null
     */
    public Long getUserIdFromToken(String token) {
        try {
            // 解析 JWT
            Claims claims = parseToken(token);
            // 从 subject 中取出用户ID（存的时候是字符串，转成 Long）
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            // 令牌无效或已过期
            return null;
        }
    }

    /**
     * 验证 JWT 令牌是否有效
     *
     * @param token JWT 字符串
     * @return true=有效，false=无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析 JWT 令牌
     *
     * @param token JWT 字符串
     * @return 令牌中的声明（Claims）
     * @throws JwtException 如果令牌无效或已过期
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
