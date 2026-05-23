package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Result<Map<String, Object>> getDashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(dashboardService.getDashboard(userId));
    }
}
