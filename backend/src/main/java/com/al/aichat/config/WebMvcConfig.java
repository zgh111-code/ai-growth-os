package com.al.aichat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * 作用：注册拦截器，配置哪些接口需要拦截、哪些不需要。
 *
 * 当前注册的拦截器：
 *   1. JwtAuthInterceptor  - JWT 认证（拦截 /api/**，排除登录/注册）
 *   2. RateLimitInterceptor - 限流（拦截聊天发送、登录、注册）
 *
 * 不需要 JWT 认证的接口：
 *   - POST /api/user/register  - 注册接口，用户还没登录
 *   - POST /api/user/login     - 登录接口
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor,
                        JwtAuthInterceptor jwtAuthInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /uploads/** → 项目根目录下的 uploads/ 文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1. JWT 认证拦截器（拦截所有 /api/** 请求，排除登录/注册）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/register", "/api/user/login", "/api/health");

        // 2. 限流拦截器（对聊天接口和认证接口进行限流）
        //    聊天接口：防止滥用 DeepSeek API，产生费用
        //    认证接口：防止暴力破解密码
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/chat/send", "/api/user/login", "/api/user/register");
    }
}
