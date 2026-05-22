package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.entity.LearningRecord;
import com.al.aichat.mapper.LearningRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class LearningService {

    private final LearningRecordMapper recordMapper;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> CATEGORIES = List.of("english", "coding", "project", "exercise");
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
        "english", "英语", "coding", "编程", "project", "项目", "exercise", "锻炼"
    );

    public LearningService(LearningRecordMapper recordMapper, DeepSeekService deepSeekService) {
        this.recordMapper = recordMapper;
        this.deepSeekService = deepSeekService;
    }

    public LearningRecord checkin(Long userId, String category, Integer durationMin, String note) {
        if (category == null || !CATEGORIES.contains(category)) {
            throw new BusinessException("无效的学习类别");
        }

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getUserId, userId)
               .eq(LearningRecord::getRecordDate, today)
               .eq(LearningRecord::getCategory, category);
        if (recordMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("今天已在该类别打过卡了");
        }

        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setCategory(category);
        record.setDurationMin(durationMin != null ? durationMin : 0);
        record.setNote(note);
        record.setRecordDate(today);
        recordMapper.insert(record);
        return record;
    }

    public Map<String, Object> getStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        LambdaQueryWrapper<LearningRecord> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(LearningRecord::getUserId, userId)
                   .ge(LearningRecord::getRecordDate, weekStart)
                   .le(LearningRecord::getRecordDate, today);
        List<LearningRecord> weekRecords = recordMapper.selectList(weekWrapper);

        Map<String, Long> categoryCount = new LinkedHashMap<>();
        Map<String, Integer> categoryMinutes = new LinkedHashMap<>();
        for (String cat : CATEGORIES) {
            List<LearningRecord> catRecords = weekRecords.stream()
                .filter(r -> cat.equals(r.getCategory())).toList();
            categoryCount.put(cat, (long) catRecords.size());
            categoryMinutes.put(cat, catRecords.stream().mapToInt(r -> r.getDurationMin() != null ? r.getDurationMin() : 0).sum());
        }

        long weekDays = weekRecords.stream().map(LearningRecord::getRecordDate).distinct().count();

        int streak = 0;
        LocalDate cursor = today;
        while (true) {
            LambdaQueryWrapper<LearningRecord> dayWrapper = new LambdaQueryWrapper<>();
            dayWrapper.eq(LearningRecord::getUserId, userId)
                       .eq(LearningRecord::getRecordDate, cursor);
            Long count = recordMapper.selectCount(dayWrapper);
            if (count == null || count == 0) break;
            streak++;
            cursor = cursor.minusDays(1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("streak", streak);
        result.put("weekDays", weekDays);
        result.put("weekTotal", weekRecords.size());
        result.put("categoryCount", categoryCount);
        result.put("categoryMinutes", categoryMinutes);
        result.put("categoryLabels", CATEGORY_LABELS);
        return result;
    }

    public List<Map<String, Object>> getCalendar(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getUserId, userId)
               .ge(LearningRecord::getRecordDate, start)
               .le(LearningRecord::getRecordDate, end);
        List<LearningRecord> records = recordMapper.selectList(wrapper);

        Map<LocalDate, Set<String>> dateCategories = new LinkedHashMap<>();
        for (LearningRecord r : records) {
            dateCategories.computeIfAbsent(r.getRecordDate(), k -> new LinkedHashSet<>())
                          .add(r.getCategory());
        }

        List<Map<String, Object>> calendar = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", d.toString());
            day.put("categories", dateCategories.getOrDefault(d, Set.of()));
            calendar.add(day);
        }
        return calendar;
    }

    /**
     * AI 分析今日学习情况
     */
    public Map<String, Object> analyzeToday(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getUserId, userId)
               .eq(LearningRecord::getRecordDate, today);
        List<LearningRecord> todayRecords = recordMapper.selectList(wrapper);

        if (todayRecords.isEmpty()) {
            throw new BusinessException("今天还没有学习记录，请先打卡");
        }

        // 整理各分类数据
        String english = buildCategorySummary(todayRecords, "english");
        String coding = buildCategorySummary(todayRecords, "coding");
        String project = buildCategorySummary(todayRecords, "project");
        String exercise = buildCategorySummary(todayRecords, "exercise");

        String systemPrompt = """
            你是学习成长分析AI。

            请根据用户今天的学习记录进行分析，输出JSON：

            {
              "learning_summary": "今日学习总结",
              "consistency_score": 75,
              "focus_analysis": "专注度分析",
              "improvement_advice": ["具体建议"]
            }

            要求：
            - 必须真实分析
            - 不要鸡汤
            - 重点关注执行力与专注度
            - consistency_score 0-100""";

        String userPrompt = String.format("英语：%s\n编程：%s\n项目：%s\n锻炼：%s",
            english, coding, project, exercise);
        String aiResponse = deepSeekService.generateText(systemPrompt, userPrompt);

        return parseAIAnalysis(aiResponse);
    }

    private String buildCategorySummary(List<LearningRecord> records, String category) {
        List<LearningRecord> catRecords = records.stream()
            .filter(r -> category.equals(r.getCategory())).toList();
        if (catRecords.isEmpty()) {
            return "未记录";
        }
        int totalMin = catRecords.stream().mapToInt(r -> r.getDurationMin() != null ? r.getDurationMin() : 0).sum();
        String summary = "学习" + totalMin + "分钟";
        for (LearningRecord r : catRecords) {
            if (r.getNote() != null && !r.getNote().isBlank()) {
                summary += "，备注：" + r.getNote();
                break;
            }
        }
        return summary;
    }

    private Map<String, Object> parseAIAnalysis(String aiResponse) {
        try {
            String json = aiResponse;
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                if (json.contains("```")) json = json.substring(0, json.lastIndexOf("```"));
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                if (json.contains("```")) json = json.substring(0, json.lastIndexOf("```"));
            }
            json = json.trim();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("learning_summary", aiResponse);
            fallback.put("consistency_score", 0);
            fallback.put("focus_analysis", "");
            fallback.put("improvement_advice", List.of());
            return fallback;
        }
    }
}
