package com.al.aichat.utils;

import com.al.aichat.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils 单元测试
 *
 * 测试 JWT 令牌的生成、解析和验证功能。
 * 直接使用真实的 JwtConfig 对象，不依赖 Mockito 或 Spring 上下文。
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    /**
     * 每个测试方法执行前创建真实对象
     * JwtConfig 只是一个简单的 POJO，直接设置值即可
     */
    @BeforeEach
    void setUp() {
        // 使用真实的 JwtConfig 实例，而不是 Mock
        JwtConfig jwtConfig = new JwtConfig();
        // 密钥：至少 256 位（32 字节），HS256 算法要求
        jwtConfig.setSecret("MySuperSecretKeyForJWTTokenGeneration2024!");
        // 过期时间：24 小时（毫秒）
        jwtConfig.setExpiration(86400000L);

        jwtUtils = new JwtUtils(jwtConfig);
    }

    @Test
    @DisplayName("生成 JWT 令牌 - 应返回有效的三部分令牌")
    void generateToken_ShouldReturnValidJwt() {
        // 执行
        String token = jwtUtils.generateToken(1L, "testuser");

        // 验证：JWT 由三个点分隔的部分组成（header.payload.signature）
        assertNotNull(token, "令牌不应为 null");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT 应包含三部分");
    }

    @Test
    @DisplayName("从令牌中提取用户ID - 应返回正确的用户ID")
    void getUserIdFromToken_ShouldReturnCorrectUserId() {
        // 执行
        String token = jwtUtils.generateToken(42L, "johndoe");
        Long userId = jwtUtils.getUserIdFromToken(token);

        // 验证
        assertEquals(42L, userId, "从令牌解析出的用户ID应与生成时一致");
    }

    @Test
    @DisplayName("验证有效令牌 - 应返回 true")
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // 执行
        String token = jwtUtils.generateToken(1L, "testuser");

        // 验证
        assertTrue(jwtUtils.validateToken(token), "有效令牌应验证通过");
    }

    @Test
    @DisplayName("验证无效令牌 - 应返回 false")
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // 执行 & 验证：随意拼凑的字符串不应通过验证
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("从无效令牌中提取用户ID - 应返回 null")
    void getUserIdFromToken_WithInvalidToken_ShouldReturnNull() {
        // 执行 & 验证
        assertNull(jwtUtils.getUserIdFromToken("invalid.token.here"));
    }

    @Test
    @DisplayName("从空字符串令牌中提取用户ID - 应返回 null")
    void getUserIdFromToken_WithEmptyToken_ShouldReturnNull() {
        assertNull(jwtUtils.getUserIdFromToken(""));
    }

    @Test
    @DisplayName("不同用户生成的令牌应不同")
    void generateToken_DifferentUsers_ShouldProduceDifferentTokens() {
        // 执行
        String token1 = jwtUtils.generateToken(1L, "alice");
        String token2 = jwtUtils.generateToken(2L, "bob");

        // 验证：两个令牌不应相同
        assertNotEquals(token1, token2, "不同用户生成的令牌不应相同");
    }
}
