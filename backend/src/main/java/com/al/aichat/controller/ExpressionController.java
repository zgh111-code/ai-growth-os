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
        Long userId = (Long) request.getAttribute("userId");
        String topic = expressionService.getTodayTopic(userId);
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

    @GetMapping("/{id}")
    public Result<ExpressionRecord> detail(@PathVariable Long id,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(expressionService.getById(id, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        expressionService.delete(id, userId);
        return Result.success("删除成功", null);
    }
}
