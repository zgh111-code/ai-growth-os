package com.al.aichat.controller;

import com.al.aichat.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * 用于监控系统运行状态，负载均衡器 / 运维脚本定期访问这个接口判断服务是否存活。
 * 不需要登录，不需要限流。
 */
@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * GET /api/health
     *
     * 返回服务健康状态，包括数据库连接是否正常。
     */
    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("service", "ai-chat");

        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            info.put("database", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            info.put("database", "DOWN");
            info.put("dbError", e.getMessage());
        }

        return Result.success(info);
    }

}
