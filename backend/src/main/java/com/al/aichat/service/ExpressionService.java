package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.entity.ExpressionRecord;
import com.al.aichat.mapper.ExpressionRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExpressionService {

    private final ExpressionRecordMapper recordMapper;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<Long, Object> topicLocks = new ConcurrentHashMap<>();

    private static final String[] FALLBACK_TOPICS = {
        "你如何看待'自律'和'自由'的关系？",
        "分享一个你最近学到的概念，并解释给一个完全不懂的人听。",
        "你认为大学教育中最缺失的是什么？",
        "描述一个你最近做过的决定，并分析你的决策过程。",
        "你理想中的工作是什么样的？为什么？",
        "讲一个你身边的榜样，他的什么品质最打动你？",
        "如果可以给大一新生一条建议，你会说什么？",
        "你认为AI会如何改变你的专业领域？",
        "描述一次你克服困难的经历，你从中学到了什么？",
        "你如何平衡学习、社交和个人时间？",
        "谈谈你对'失败是成功之母'的理解",
        "你认为什么样的人值得深交？",
        "如果让你给全校同学做一次演讲，你会讲什么主题？",
        "谈谈你对当前社交媒体的看法",
        "你认为'天赋'和'努力'哪个更重要？",
        "描述一个改变你世界观的事件或经历",
        "你如何看待'内卷'和'躺平'这两种态度？",
        "如果大学可以重来，你会改变什么？",
        "谈谈你对'终身学习'的理解",
        "你认为什么样的师生关系是理想的？",
        "分享一个你最近关注的时事热点，并谈谈你的看法",
        "你认为大学期间最应该培养的能力是什么？",
        "谈谈你对'舒适区'的理解",
        "如果可以掌握一项新技能，你会选什么？为什么？",
        "你认为一个人最可贵的品质是什么？",
        "谈谈你对未来的规划和期待",
        "分享一本对你影响很大的书，它改变了你什么？",
        "你如何看待团队合作中的冲突？",
        "谈谈你对'活在当下'这句话的理解",
        "你认为什么是真正的朋友？",
    };

    public ExpressionService(ExpressionRecordMapper recordMapper, DeepSeekService deepSeekService) {
        this.recordMapper = recordMapper;
        this.deepSeekService = deepSeekService;
    }

    public String getTodayTopic(Long userId) {
        ExpressionRecord todayRecord = getTodayRecord(userId);
        if (todayRecord != null) {
            return todayRecord.getTopic();
        }
        // 同步避免并发时重复插入
        Object lock = topicLocks.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            todayRecord = getTodayRecord(userId);
            if (todayRecord != null) {
                return todayRecord.getTopic();
            }
            String topic = generateTopic(userId);
            ExpressionRecord record = new ExpressionRecord();
            record.setUserId(userId);
            record.setTopic(topic);
            recordMapper.insert(record);
            return topic;
        }
    }

    private ExpressionRecord getTodayRecord(Long userId) {
        LambdaQueryWrapper<ExpressionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpressionRecord::getUserId, userId)
               .ge(ExpressionRecord::getCreatedAt, LocalDate.now().atStartOfDay())
               .lt(ExpressionRecord::getCreatedAt, LocalDate.now().plusDays(1).atStartOfDay())
               .orderByAsc(ExpressionRecord::getCreatedAt)
               .last("LIMIT 1");
        return recordMapper.selectOne(wrapper);
    }

    private String generateTopic(Long userId) {
        List<String> usedTopics = getUsedTopics(userId);
        try {
            String prompt = "生成一个适合大学生练习逻辑表达的话题，要求：能引发深度思考，适合用3-5分钟口头表达。只返回话题本身，不要解释。";
            if (!usedTopics.isEmpty()) {
                prompt += " 注意：绝对不要重复以下已用过的话题——" + String.join("；", usedTopics);
            }
            String topic = deepSeekService.generateText("你是表达训练导师，负责生成有深度的表达话题。", prompt).trim();
            // 如果 AI 仍返回了重复话题，回退到兜底列表
            if (usedTopics.contains(topic)) {
                return fallbackTopic(usedTopics);
            }
            return topic;
        } catch (Exception e) {
            return fallbackTopic(usedTopics);
        }
    }

    private String fallbackTopic(List<String> usedTopics) {
        for (String topic : FALLBACK_TOPICS) {
            if (!usedTopics.contains(topic)) {
                return topic;
            }
        }
        // 所有话题都用过了，基于日期生成
        return "今天是你第" + (usedTopics.size() + 1) + "次表达训练，请自由选择一个话题，谈谈你最近的思考与感悟。";
    }

    private List<String> getUsedTopics(Long userId) {
        LambdaQueryWrapper<ExpressionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ExpressionRecord::getTopic)
               .eq(ExpressionRecord::getUserId, userId)
               .isNotNull(ExpressionRecord::getContent);
        return recordMapper.selectList(wrapper).stream()
                .map(ExpressionRecord::getTopic)
                .toList();
    }

    public ExpressionRecord submit(Long userId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("表达内容不能为空");
        }
        // 确保今天已有占位记录（getTodayTopic 会自动创建）
        getTodayTopic(userId);
        ExpressionRecord record = getTodayRecord(userId);
        record.setContent(content);

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
            "表达主题：" + record.getTopic() + "\n用户表达内容：" + content);

        parseAIResponse(aiResponse, record);
        recordMapper.updateById(record);
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
               .isNotNull(ExpressionRecord::getContent)
               .orderByDesc(ExpressionRecord::getCreatedAt);
        return recordMapper.selectList(wrapper);
    }

    public ExpressionRecord getById(Long id, Long userId) {
        LambdaQueryWrapper<ExpressionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpressionRecord::getId, id)
               .eq(ExpressionRecord::getUserId, userId);
        ExpressionRecord record = recordMapper.selectOne(wrapper);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return record;
    }

    public void delete(Long id, Long userId) {
        LambdaQueryWrapper<ExpressionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpressionRecord::getId, id)
               .eq(ExpressionRecord::getUserId, userId);
        if (recordMapper.delete(wrapper) == 0) {
            throw new BusinessException("记录不存在");
        }
    }
}
