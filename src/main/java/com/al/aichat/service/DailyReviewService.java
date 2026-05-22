package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.entity.DailyReview;
import com.al.aichat.mapper.DailyReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class DailyReviewService {

    private final DailyReviewMapper reviewMapper;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DailyReviewService(DailyReviewMapper reviewMapper, DeepSeekService deepSeekService) {
        this.reviewMapper = reviewMapper;
        this.deepSeekService = deepSeekService;
    }

    public DailyReview submit(Long userId, String content, String mood) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("复盘内容不能为空");
        }

        String systemPrompt = """
            你是一个大学生成长分析AI。

            请分析以下每日复盘内容：

            用户输入：
            {content}

            请严格输出JSON：

            {
              "summary": "今天总体情况总结",
              "positives": [
                "做得好的地方"
              ],
              "problems": [
                "存在的问题"
              ],
              "suggestions": [
                "明天可执行建议"
              ]
            }

            要求：
            - 不要空话
            - 必须具体
            - 建议必须当天可执行
            - 禁止"继续加油""提升能力"这种泛化表达""";

        String userPrompt = "今日复盘内容：" + content + (mood != null ? "\n情绪状态：" + mood : "");
        String aiResponse = deepSeekService.generateText(systemPrompt, userPrompt);

        DailyReview review = new DailyReview();
        review.setUserId(userId);
        review.setContent(content);
        review.setMood(mood);
        review.setReviewDate(LocalDate.now());

        parseAIResponse(aiResponse, review);
        reviewMapper.insert(review);
        return review;
    }

    private void parseAIResponse(String aiResponse, DailyReview review) {
        try {
            String json = aiResponse;
            // 处理可能的 markdown 代码块包裹
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                if (json.contains("```")) {
                    json = json.substring(0, json.lastIndexOf("```"));
                }
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                if (json.contains("```")) {
                    json = json.substring(0, json.lastIndexOf("```"));
                }
            }
            json = json.trim();

            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            review.setAiSummary((String) parsed.get("summary"));
            review.setAiPositives(objectMapper.writeValueAsString(parsed.get("positives")));
            review.setAiProblems(objectMapper.writeValueAsString(parsed.get("problems")));
            review.setAiSuggestions(objectMapper.writeValueAsString(parsed.get("suggestions")));
        } catch (Exception e) {
            review.setAiSummary(aiResponse);
            review.setAiPositives("[]");
            review.setAiProblems("[]");
            review.setAiSuggestions("[]");
        }
    }

    public List<DailyReview> list(Long userId) {
        LambdaQueryWrapper<DailyReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyReview::getUserId, userId)
               .orderByDesc(DailyReview::getReviewDate);
        return reviewMapper.selectList(wrapper);
    }

    public DailyReview getById(Long id, Long userId) {
        DailyReview review = reviewMapper.selectById(id);
        if (review == null || !review.getUserId().equals(userId)) {
            throw new BusinessException(404, "复盘记录不存在");
        }
        return review;
    }
}
