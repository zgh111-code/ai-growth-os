package com.al.aichat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 聊天平台 - 启动类
 *
 * 这是整个项目的入口，运行这个类的 main 方法就能启动项目。
 *
 * @SpringBootApplication 是一个组合注解，包含：
 *   1. @Configuration       - 标记这是一个配置类
 *   2. @EnableAutoConfiguration - 自动配置 SpringBoot
 *   3. @ComponentScan        - 自动扫描当前包及其子包下的组件
 */
@SpringBootApplication
public class AiChatApplication {

    public static void main(String[] args) {
        // 启动 SpringBoot 应用
        SpringApplication.run(AiChatApplication.class, args);
    }

}
