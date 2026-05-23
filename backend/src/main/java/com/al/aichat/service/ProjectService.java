package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.entity.ProjectRecord;
import com.al.aichat.mapper.ProjectRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    private final ProjectRecordMapper recordMapper;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProjectService(ProjectRecordMapper recordMapper, DeepSeekService deepSeekService) {
        this.recordMapper = recordMapper;
        this.deepSeekService = deepSeekService;
    }

    public ProjectRecord submit(Long userId, String projectName, String status, String blocker, String progress) {
        if (projectName == null || projectName.isBlank()) {
            throw new BusinessException("项目名称不能为空");
        }
        if (status == null || status.isBlank()) {
            throw new BusinessException("项目状态不能为空");
        }

        String systemPrompt = """
            你是AI项目推进助手。

            用户会描述：
            - 当前项目状态
            - 当前卡点
            - 最近进度

            请帮助用户：
            1. 拆解下一步任务
            2. 给出优先级
            3. 避免同时做太多功能
            4. 提醒风险

            输出JSON：

            {
              "current_focus": "当前最应该做的事",
              "next_tasks": [
                {
                  "task": "任务内容",
                  "priority": "high/medium/low"
                }
              ],
              "risks": [
                "当前风险"
              ],
              "advice": [
                "执行建议"
              ]
            }

            要求：
            - 必须现实
            - 避免空泛
            - 强调MVP开发
            - 不允许建议用户无限扩展功能""";

        String userPrompt = String.format("项目名称：%s\n当前状态：%s\n卡点：%s\n今日进度：%s",
            projectName, status,
            blocker != null ? blocker : "无",
            progress != null ? progress : "无");

        String aiResponse = deepSeekService.generateText(systemPrompt, userPrompt);

        ProjectRecord record = new ProjectRecord();
        record.setUserId(userId);
        record.setProjectName(projectName);
        record.setStatus(status);
        record.setBlocker(blocker);
        record.setProgress(progress);
        parseAIResponse(aiResponse, record);
        recordMapper.insert(record);
        return record;
    }

    private void parseAIResponse(String aiResponse, ProjectRecord record) {
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
            record.setAiCurrentFocus((String) parsed.get("current_focus"));
            record.setAiNextTasks(objectMapper.writeValueAsString(parsed.get("next_tasks")));
            record.setAiRisks(objectMapper.writeValueAsString(parsed.get("risks")));
            record.setAiAdvice(objectMapper.writeValueAsString(parsed.get("advice")));
        } catch (Exception e) {
            record.setAiCurrentFocus(aiResponse);
            record.setAiNextTasks("[]");
            record.setAiRisks("[]");
            record.setAiAdvice("[]");
        }
    }

    public List<ProjectRecord> list(Long userId) {
        LambdaQueryWrapper<ProjectRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectRecord::getUserId, userId)
               .orderByDesc(ProjectRecord::getCreatedAt);
        return recordMapper.selectList(wrapper);
    }

    public ProjectRecord getById(Long id, Long userId) {
        ProjectRecord record = recordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "项目记录不存在");
        }
        return record;
    }
}
