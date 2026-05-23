package com.al.aichat.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.al.aichat.mapper")
public class MyBatisConfig {
}
