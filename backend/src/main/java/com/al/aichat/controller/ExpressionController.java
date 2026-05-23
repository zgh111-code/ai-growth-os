package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.entity.ExpressionRecord;
import com.al.aichat.service.ExpressionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expression")
public class ExpressionController {

    private final ExpressionService expressionService;

    public ExpressionController(ExpressionService expressionService) {
        this.expressionService = expressionService;
    }

    @GetMapping("/today")
    public Result<Map<String, String>> getTodayTopic(HttpServletRequest request) {
        String topic = expressionService.getTodayTopic();
        return Result.success(Map.of("topic", topic));
    }

    @PostMapping("/submit")
    public Result<ExpressionRecord> submit(@RequestBody Map<String, String> body,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String content = body.get("content");
        ExpressionRecord record = expressionService.submit(userId, content);
        return Result.success("分析完成", record);
    }

    @GetMapping("/history")
    public Result<List<ExpressionRecord>> history(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(expressionService.history(userId));
    }
}
