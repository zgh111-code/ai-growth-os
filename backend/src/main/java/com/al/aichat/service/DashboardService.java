package com.al.aichat.service;

import com.al.aichat.entity.*;
import com.al.aichat.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class DashboardService {

    private final LearningRecordMapper learningRecordMapper;
    private final DailyReviewMapper reviewMapper;
    private final ExpressionRecordMapper expressionMapper;
    private final ProjectRecordMapper projectMapper;

    public DashboardService(LearningRecordMapper learningRecordMapper,
                            DailyReviewMapper reviewMapper,
                            ExpressionRecordMapper expressionMapper,
                            ProjectRecordMapper projectMapper) {
        this.learningRecordMapper = learningRecordMapper;
        this.reviewMapper = reviewMapper;
        this.expressionMapper = expressionMapper;
        this.projectMapper = projectMapper;
    }

    public Map<String, Object> getDashboard(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 连续打卡天数
        int streak = calcStreak(userId, today);

        // 本周每天活动状态
        LambdaQueryWrapper<LearningRecord> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(LearningRecord::getUserId, userId)
                   .ge(LearningRecord::getRecordDate, weekStart)
                   .le(LearningRecord::getRecordDate, today);
        Set<LocalDate> activeDays = new HashSet<>();
        for (LearningRecord lr : learningRecordMapper.selectList(weekWrapper)) {
            activeDays.add(lr.getRecordDate());
        }
        int weekDays = activeDays.size();

        // 周一=0 ... 周日=6 的布尔数组
        List<Boolean> weekActivity = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            weekActivity.add(activeDays.contains(d));
        }

        // 复盘记录数
        Long reviewCount = reviewMapper.selectCount(
            new LambdaQueryWrapper<DailyReview>().eq(DailyReview::getUserId, userId));

        // 表达训练数
        Long expressionCount = expressionMapper.selectCount(
            new LambdaQueryWrapper<ExpressionRecord>().eq(ExpressionRecord::getUserId, userId));

        // 今日所有记录条数
        int todayCount = 0;
        todayCount += learningRecordMapper.selectCount(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getRecordDate, today)).intValue();
        todayCount += reviewMapper.selectCount(
            new LambdaQueryWrapper<DailyReview>()
                .eq(DailyReview::getUserId, userId)
                .ge(DailyReview::getCreatedAt, today.atStartOfDay())).intValue();
        todayCount += expressionMapper.selectCount(
            new LambdaQueryWrapper<ExpressionRecord>()
                .eq(ExpressionRecord::getUserId, userId)
                .ge(ExpressionRecord::getCreatedAt, today.atStartOfDay())).intValue();
        todayCount += projectMapper.selectCount(
            new LambdaQueryWrapper<ProjectRecord>()
                .eq(ProjectRecord::getUserId, userId)
                .ge(ProjectRecord::getCreatedAt, today.atStartOfDay())).intValue();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("streak", streak);
        stats.put("weekDays", weekDays);
        stats.put("weekActivity", weekActivity);
        stats.put("todayCount", todayCount);
        stats.put("reviewCount", reviewCount != null ? reviewCount.intValue() : 0);
        stats.put("expressionCount", expressionCount != null ? expressionCount.intValue() : 0);

        // 最近记录
        List<Map<String, Object>> recent = buildRecentFeed(userId, today);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", stats);
        result.put("recent", recent);
        return result;
    }

    private int calcStreak(Long userId, LocalDate from) {
        int s = 0;
        LocalDate cursor = from;
        while (true) {
            Long count = learningRecordMapper.selectCount(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .eq(LearningRecord::getRecordDate, cursor));
            if (count == null || count == 0) break;
            s++;
            cursor = cursor.minusDays(1);
        }
        return s;
    }

    private List<Map<String, Object>> buildRecentFeed(Long userId, LocalDate today) {
        List<Map<String, Object>> feed = new ArrayList<>();

        // 最新复盘
        List<DailyReview> reviews = reviewMapper.selectList(
            new LambdaQueryWrapper<DailyReview>()
                .eq(DailyReview::getUserId, userId)
                .orderByDesc(DailyReview::getCreatedAt)
                .last("LIMIT 3"));
        for (DailyReview r : reviews) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "r-" + r.getId());
            item.put("type", "review");
            item.put("text", r.getAiSummary() != null ? shortText(r.getAiSummary(), 80) : shortText(r.getContent(), 80));
            item.put("time", formatTime(r.getCreatedAt()));
            feed.add(item);
        }

        // 最新学习打卡
        List<LearningRecord> learnings = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .orderByDesc(LearningRecord::getCreatedAt)
                .last("LIMIT 3"));
        for (LearningRecord l : learnings) {
            String cat = Map.of("english","英语","coding","编程","project","项目","exercise","锻炼")
                .getOrDefault(l.getCategory(), l.getCategory());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "l-" + l.getId());
            item.put("type", "learning");
            item.put("text", cat + " · " + (l.getDurationMin() != null ? l.getDurationMin() + "分钟" : "已打卡"));
            item.put("time", formatTime(l.getCreatedAt()));
            feed.add(item);
        }

        // 最新项目记录
        List<ProjectRecord> projects = projectMapper.selectList(
            new LambdaQueryWrapper<ProjectRecord>()
                .eq(ProjectRecord::getUserId, userId)
                .orderByDesc(ProjectRecord::getCreatedAt)
                .last("LIMIT 2"));
        for (ProjectRecord p : projects) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "p-" + p.getId());
            item.put("type", "project");
            item.put("text", p.getProjectName() + ": " + shortText(p.getAiCurrentFocus() != null ? p.getAiCurrentFocus() : p.getStatus(), 60));
            item.put("time", formatTime(p.getCreatedAt()));
            feed.add(item);
        }

        feed.sort((a, b) -> ((String) b.get("time")).compareTo((String) a.get("time")));
        return feed.size() > 10 ? feed.subList(0, 10) : feed;
    }

    private String shortText(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
    }

    private String formatTime(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        LocalDate today = LocalDate.now();
        if (dt.toLocalDate().equals(today)) {
            return String.format("今天 %02d:%02d", dt.getHour(), dt.getMinute());
        } else if (dt.toLocalDate().equals(today.minusDays(1))) {
            return String.format("昨天 %02d:%02d", dt.getHour(), dt.getMinute());
        }
        return dt.toLocalDate().toString();
    }
}
