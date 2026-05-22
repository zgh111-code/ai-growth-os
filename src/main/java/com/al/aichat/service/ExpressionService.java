package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.entity.ExpressionRecord;
import com.al.aichat.mapper.ExpressionRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExpressionService {

    private final ExpressionRecordMapper recordMapper;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<LocalDate, String> topicCache = new ConcurrentHashMap<>();

    private static final String[] FALLBACK_TOPICS = {
        "你如何看待'自律'和'自由'的关系？",
        "分享一个你最近学到的概念，并解释给一个完全不懂的人听。",
        "你认为大学教育中最缺失的是什么？",
        "描述一个你最近做过的决定，并分析你的决策过程。",
        "你理想中的工作是什么样的？为什么？",
        "讲一个你身边的榜样，他的什么品质最打动你？",
    };

    public ExpressionService(ExpressionRecordMapper recordMapper, DeepSeekService deepSeekService) {
        this.recordMapper = recordMapper;
        this.deepSeekService = deepSeekService;
    }

    public String getTodayTopic() {
        LocalDate today = LocalDate.now();
        return topicCache.computeIfAbsent(today, d -> generateTopic());
    }

    private String generateTopic() {
        try {
            String prompt = "生成一个适合大学生练习逻辑表达的话题，要求：能引发深度思考，适合用3-5分钟口头表达。只返回话题本身，不要解释。";
            return deepSeekService.generateText("你是表达训练导师，负责生成有深度的表达话题。", prompt).trim();
        } catch (Exception e) {
            return FALLBACK_TOPICS[(int) (System.currentTimeMillis() % FALLBACK_TOPICS.length)];
        }
    }

    public ExpressionRecord submit(Long userId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("表达内容不能为空");
        }
        String topic = getTodayTopic();

        String systemPrompt = """
            你是表达能力训练AI。

            用户会输入一段自己的表达内容。

            请分析：
            1. 表达逻辑是否清晰
            2. 是否存在重复表达
            3. 是否想到哪说到哪
            4. 是否缺少结构
            5. 如何优化

            请严格输出JSON：

            {
              "logic_score": 0-100,
              "clarity_score": 0-100,
              "problems": [
                "表达问题"
              ],
              "optimized_expression": "优化后的表达示例",
              "suggestions": [
                "训练建议"
              ]
            }

            要求：
            - 直接指出问题
            - 不要鼓励式废话
            - 必须具体""";

        String aiResponse = deepSeekService.generateText(systemPrompt,
            "表达主题：" + topic + "\n用户表达内容：" + content);

        ExpressionRecord record = new ExpressionRecord();
        record.setUserId(userId);
        record.setTopic(topic);
        record.setContent(content);
        parseAIResponse(aiResponse, record);
        recordMapper.insert(record);
        return record;
    }

    private void parseAIResponse(String aiResponse, ExpressionRecord record) {
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

            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            if (parsed.get("logic_score") instanceof Number) {
                record.setAiLogicScore(((Number) parsed.get("logic_score")).intValue());
            }
            if (parsed.get("clarity_score") instanceof Number) {
                record.setAiClarityScore(((Number) parsed.get("clarity_score")).intValue());
            }
            record.setAiProblems(objectMapper.writeValueAsString(parsed.get("problems")));
            record.setAiOptimizedExpression((String) parsed.get("optimized_expression"));
            record.setAiSuggestions(objectMapper.writeValueAsString(parsed.get("suggestions")));
        } catch (Exception e) {
            record.setAiLogicScore(0);
            record.setAiClarityScore(0);
            record.setAiProblems("[]");
            record.setAiOptimizedExpression("");
            record.setAiSuggestions("[\"" + aiResponse.replace("\"", "\\\"") + "\"]");
        }
    }

    public List<ExpressionRecord> history(Long userId) {
        LambdaQueryWrapper<ExpressionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpressionRecord::getUserId, userId)
               .orderByDesc(ExpressionRecord::getCreatedAt);
        return recordMapper.selectList(wrapper);
    }
}
