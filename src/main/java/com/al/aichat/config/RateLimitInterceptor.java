package com.al.aichat.config;

import com.al.aichat.common.Result;
import com.al.aichat.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int USER_MAX_REQUESTS = 10;
    private static final int IP_MAX_REQUESTS = 20;
    private static final int AUTH_IP_MAX_REQUESTS = 5;
    private static final long TIME_WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, Integer> userCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> ipCountMap = new ConcurrentHashMap<>();

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        long currentTime = System.currentTimeMillis();
        long windowStart = (currentTime / TIME_WINDOW_MS) * TIME_WINDOW_MS;

        boolean isAuthEndpoint = request.getRequestURI().contains("/api/user/login")
                || request.getRequestURI().contains("/api/user/register");

        int ipMaxRequests = isAuthEndpoint ? AUTH_IP_MAX_REQUESTS : IP_MAX_REQUESTS;

        // IP 限流（原子操作）
        String clientIp = getClientIp(request);
        String ipKey = clientIp + ":" + windowStart;

        int ipCount = ipCountMap.merge(ipKey, 1, Integer::sum);
        if (ipCount > ipMaxRequests) {
            String msg = isAuthEndpoint
                    ? "登录尝试过于频繁，请1分钟后再试"
                    : "请求过于频繁，请稍后再试";
            writeRateLimitResponse(response, msg);
            return false;
        }

        // 用户限流（仅对已登录用户生效）
        Long userId = getUserIdFromRequest(request);
        if (userId != null) {
            String userKey = userId + ":" + windowStart;

            int userCount = userCountMap.merge(userKey, 1, Integer::sum);
            if (userCount > USER_MAX_REQUESTS) {
                writeRateLimitResponse(response, "发送消息太频繁，请稍后再试");
                return false;
            }
        }

        if (ipCountMap.size() > 10000) {
            cleanExpiredEntries();
        }

        return true;
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return null;
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void writeRateLimitResponse(HttpServletResponse response, String message)
            throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");

        Result<?> result = Result.error(429, message);
        String json = objectMapper.writeValueAsString(result);

        response.getWriter().write(json);
    }

    private void cleanExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        long oldestAllowed = currentTime - TIME_WINDOW_MS;

        ipCountMap.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            String timestampStr = key.substring(key.lastIndexOf(':') + 1);
            try {
                long timestamp = Long.parseLong(timestampStr);
                return timestamp < oldestAllowed;
            } catch (NumberFormatException e) {
                return true;
            }
        });

        userCountMap.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            String timestampStr = key.substring(key.lastIndexOf(':') + 1);
            try {
                long timestamp = Long.parseLong(timestampStr);
                return timestamp < oldestAllowed;
            } catch (NumberFormatException e) {
                return true;
            }
        });
    }
}
