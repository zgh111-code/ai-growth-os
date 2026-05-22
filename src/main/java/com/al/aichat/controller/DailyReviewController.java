package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.entity.DailyReview;
import com.al.aichat.service.DailyReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/daily-review")
public class DailyReviewController {

    private final DailyReviewService reviewService;

    public DailyReviewController(DailyReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/submit")
    public Result<DailyReview> submit(@RequestBody Map<String, String> body,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String content = body.get("content");
        String mood = body.get("mood");
        DailyReview review = reviewService.submit(userId, content, mood);
        return Result.success("复盘分析完成", review);
    }

    @GetMapping("/list")
    public Result<List<DailyReview>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reviewService.list(userId));
    }

    @GetMapping("/{id}")
    public Result<DailyReview> getById(@PathVariable Long id,
                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reviewService.getById(id, userId));
    }
}
