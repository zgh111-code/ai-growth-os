package com.al.aichat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 * 允许前端页面（localhost:5173）访问后端 API（localhost:8080）
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许前端地址访问（Vite 开发服务器常用端口）
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:5174");
        config.addAllowedOrigin("http://localhost:5175");
        // 允许携带认证信息（JWT token）
        config.setAllowCredentials(true);
        // 允许所有请求方法（GET, POST, PUT, DELETE 等）
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 暴露响应头（前端可以获取）
        config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有 API 路径生效
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
