package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.config.DeepSeekConfig;
import com.al.aichat.dto.ChatRequest;
import com.al.aichat.dto.ChatRequest.Message;
import com.al.aichat.dto.ChatResponse;
import com.al.aichat.entity.ChatMessage;
import com.al.aichat.entity.ChatSession;
import com.al.aichat.mapper.ChatMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final DeepSeekConfig deepSeekConfig;
    private final ChatMessageMapper chatMessageMapper;
    private final SessionService sessionService;
    private final RestTemplate restTemplate;

    public DeepSeekService(DeepSeekConfig deepSeekConfig, ChatMessageMapper chatMessageMapper,
                           SessionService sessionService) {
        this.deepSeekConfig = deepSeekConfig;
        this.chatMessageMapper = chatMessageMapper;
        this.sessionService = sessionService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);

        log.info("DeepSeekService 初始化完成，API URL: {}", deepSeekConfig.getApiUrl());
    }

    public String chat(Long userId, Long sessionId, String content) {
        sessionService.validateOwnership(sessionId, userId);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setUserId(userId);
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);

        List<ChatMessage> history = sessionService.getRecentHistory(userId, sessionId, 10);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", "你是一个智能AI助手，请用中文回答用户的问题。"));
        for (ChatMessage msg : history) {
            // 仅使用 role 和 content 构建消息，确保不包含 thinking 字段
            messages.add(new Message(msg.getRole(), msg.getContent()));
        }

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(deepSeekConfig.getModel());
        chatRequest.setMessages(messages);
        chatRequest.setTemperature(0.7);
        chatRequest.setMaxTokens(2048);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());

        HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(chatRequest, headers);

        log.info("正在调用 DeepSeek API，用户ID: {}, 消息长度: {}", userId, content.length());
        ChatResponse chatResponse = callApiWithRetry(requestEntity);
        String reply = chatResponse.getChoices().get(0).getMessage().getContent();
        log.info("DeepSeek API 调用成功，回复长度: {}", reply.length());

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setUserId(userId);
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        assistantMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(assistantMessage);

        ChatSession session = sessionService.getById(sessionId);
        if (session != null && "新对话".equals(session.getTitle())) {
            String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            sessionService.updateTitle(sessionId, title);
        }

        return reply;
    }

    public String ragChat(Long userId, Long sessionId, String augmentedPrompt) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setUserId(userId);
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(augmentedPrompt);
        userMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);

        List<ChatMessage> history = sessionService.getRecentHistory(userId, sessionId, 5);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", "你是知识库问答助手。用户消息格式是'知识库内容：...\n问题：...'。请基于知识库内容回答。如果找不到相关信息，说'未找到相关信息'。"));
        for (ChatMessage msg : history) {
            messages.add(new Message(msg.getRole(), msg.getContent()));
        }

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(deepSeekConfig.getModel());
        chatRequest.setMessages(messages);
        chatRequest.setTemperature(0.3);
        chatRequest.setMaxTokens(2048);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());
        HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(chatRequest, headers);
        ChatResponse chatResponse = callApiWithRetry(requestEntity);
        String reply = chatResponse.getChoices().get(0).getMessage().getContent();

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setUserId(userId);
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        assistantMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(assistantMessage);

        return reply;
    }

    /**
     * 通用 AI 文本生成（供各成长模块复用，不保存聊天记录）
     */
    public String generateText(String systemPrompt, String userPrompt) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", userPrompt));

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(deepSeekConfig.getModel());
        chatRequest.setMessages(messages);
        chatRequest.setTemperature(0.3);
        chatRequest.setMaxTokens(2048);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());
        HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(chatRequest, headers);

        ChatResponse chatResponse = callApiWithRetry(requestEntity);
        return chatResponse.getChoices().get(0).getMessage().getContent();
    }

    private ChatResponse callApiWithRetry(HttpEntity<ChatRequest> requestEntity) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                ResponseEntity<ChatResponse> responseEntity = restTemplate.postForEntity(
                        deepSeekConfig.getApiUrl(), requestEntity, ChatResponse.class);
                ChatResponse chatResponse = responseEntity.getBody();
                if (chatResponse == null || chatResponse.getChoices() == null || chatResponse.getChoices().isEmpty()) {
                    throw new BusinessException("AI 服务返回异常，请稍后重试");
                }
                return chatResponse;
            } catch (BusinessException e) {
                throw e; // 业务异常不重试
            } catch (Exception e) {
                if (attempt == 0) {
                    log.warn("AI 调用失败，1秒后重试: {}", e.getMessage());
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                } else {
                    log.error("AI 调用重试后仍失败: {}", e.getMessage());
                    throw new BusinessException("AI 服务暂时不可用，请稍后重试");
                }
            }
        }
        throw new BusinessException("AI 服务暂时不可用，请稍后重试");
    }
}
