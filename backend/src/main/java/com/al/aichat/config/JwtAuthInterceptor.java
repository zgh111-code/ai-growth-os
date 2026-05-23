package com.al.aichat.config;

import com.al.aichat.common.Result;
import com.al.aichat.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 认证拦截器
 *
 * 作用：统一验证所有需要登录的接口的 JWT Token，
 *       将 userId 提取出来存入 request 属性，Controller 直接使用。
 *
 * 这样做的优势：
 *   1. 消除 3 个 Controller 中重复的 token 解析代码
 *   2. 认证逻辑集中管理，改动只需改一处
 *   3. 避免遗漏某个接口忘记验证 token
 *
 * 不需要 token 的接口（在 WebMvcConfig 中配置排除路径）：
 *   - POST /api/user/register
 *   - POST /api/user/login
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        // 从请求头获取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        String token = authHeader.substring(7);

        // 验证 token 有效性
        if (!jwtUtils.validateToken(token)) {
            writeUnauthorized(response, "token 无效或已过期，请重新登录");
            return false;
        }

        // 从 token 中提取 userId，存入 request 属性
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            writeUnauthorized(response, "token 解析失败");
            return false;
        }

        request.setAttribute("userId", userId);
        return true;
    }

    /**
     * 返回 401 未授权的 JSON 响应
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Result<?> result = Result.error(401, message);
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);
    }
}