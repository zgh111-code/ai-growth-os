package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.entity.LearningRecord;
import com.al.aichat.service.LearningService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @PostMapping("/checkin")
    public Result<LearningRecord> checkin(@RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String category = (String) body.get("category");
        Integer durationMin = body.get("durationMin") instanceof Number
            ? ((Number) body.get("durationMin")).intValue() : null;
        String note = (String) body.get("note");
        LearningRecord record = learningService.checkin(userId, category, durationMin, note);
        return Result.success("打卡成功", record);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(learningService.getStats(userId));
    }

    @GetMapping("/calendar")
    public Result<List<Map<String, Object>>> getCalendar(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (year == 0) year = java.time.LocalDate.now().getYear();
        if (month == 0) month = java.time.LocalDate.now().getMonthValue();
        return Result.success(learningService.getCalendar(userId, year, month));
    }

    @GetMapping("/analysis")
    public Result<Map<String, Object>> getAnalysis(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(learningService.analyzeToday(userId));
    }
}
