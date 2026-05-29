package com.al.aichat.service;

import com.al.aichat.config.DeepSeekConfig;
import com.al.aichat.dto.ChatRequest;
import com.al.aichat.dto.ChatRequest.Message;
import com.al.aichat.entity.ChatMessage;
import com.al.aichat.entity.ChatSession;
import com.al.aichat.mapper.ChatMessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DeepSeekStreamService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekStreamService.class);

    private final DeepSeekConfig deepSeekConfig;
    private final ChatMessageMapper chatMessageMapper;
    private final SessionService sessionService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @org.springframework.beans.factory.annotation.Autowired
    public DeepSeekStreamService(DeepSeekConfig deepSeekConfig,
                                  ChatMessageMapper chatMessageMapper,
                                  SessionService sessionService) {
        this.deepSeekConfig = deepSeekConfig;
        this.chatMessageMapper = chatMessageMapper;
        this.sessionService = sessionService;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 测试环境使用的构造函数，允许注入 mock 对象
     */
    DeepSeekStreamService(DeepSeekConfig deepSeekConfig,
                           ChatMessageMapper chatMessageMapper,
                           SessionService sessionService,
                           RestTemplate restTemplate,
                           ObjectMapper objectMapper) {
        this.deepSeekConfig = deepSeekConfig;
        this.chatMessageMapper = chatMessageMapper;
        this.sessionService = sessionService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public SseEmitter streamChat(Long userId, Long sessionId, String content) {
        SseEmitter emitter = new SseEmitter(300_000L);

        emitter.onCompletion(() -> log.info("SSE 流式响应完成，sessionId: {}", sessionId));
        emitter.onError(ex -> log.error("SSE 流式响应出错，sessionId: {}: {}", sessionId, ex.getMessage()));
        emitter.onTimeout(() -> log.warn("SSE 流式响应超时，sessionId: {}", sessionId));

        sessionService.validateOwnership(sessionId, userId);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setUserId(userId);
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);

        executor.submit(() -> {
            try {
                doStreamChat(emitter, userId, sessionId, content);
            } catch (Exception e) {
                log.error("流式聊天异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("AI 回复中断，请稍后重试"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void doStreamChat(SseEmitter emitter, Long userId, Long sessionId, String content) throws Exception {
        List<ChatMessage> history = sessionService.getRecentHistory(userId, sessionId, 10);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", "你是一个智能AI助手，请用中文回答用户的问题。"));
        for (ChatMessage msg : history) {
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

        String baseJson = objectMapper.writeValueAsString(chatRequest);
        String finalRequestJson = baseJson.substring(0, baseJson.length() - 1) + ",\"stream\":true}";

        log.info("开始流式调用 DeepSeek API，用户ID: {}", userId);

        executeWithRetry(headers, finalRequestJson, response -> {
                    StringBuilder fullReply = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data: ")) {
                                continue;
                            }

                            String data = line.substring(6).trim();

                            if ("[DONE]".equals(data)) {
                                break;
                            }

                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                JsonNode delta = jsonNode.path("choices").get(0).path("delta");
                                String contentDelta = delta.path("content").asText(null);

                                if (contentDelta != null) {
                                    fullReply.append(contentDelta);
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(contentDelta));
                                }
                            } catch (Exception e) {
                                log.warn("解析流式响应片段失败: {}", data, e);
                            }
                        }
                    }

                    String reply = fullReply.toString();
                    if (!reply.isEmpty()) {
                        ChatMessage assistantMessage = new ChatMessage();
                        assistantMessage.setUserId(userId);
                        assistantMessage.setSessionId(sessionId);
                        assistantMessage.setRole("assistant");
                        assistantMessage.setContent(reply);
                        assistantMessage.setCreatedAt(LocalDateTime.now());
                        chatMessageMapper.insert(assistantMessage);

                        log.info("流式回复完成并保存，sessionId: {}, 长度: {}", sessionId, reply.length());
                    }

                    ChatSession session = sessionService.getById(sessionId);
                    if (session != null && "新对话".equals(session.getTitle())) {
                        String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
                        sessionService.updateTitle(sessionId, title);
                    }

                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();

                    return null;
                }
        );
    }

    private void executeWithRetry(HttpHeaders headers, String body,
                                   org.springframework.web.client.ResponseExtractor<Void> extractor)
            throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                restTemplate.execute(
                    deepSeekConfig.getApiUrl(),
                    org.springframework.http.HttpMethod.POST,
                    request -> {
                        request.getHeaders().putAll(headers);
                        request.getBody().write(body.getBytes(StandardCharsets.UTF_8));
                    },
                    extractor
                );
                return;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                if (attempt == 0) {
                    log.warn("DeepSeek API 第1次调用失败（{}），1秒后重试...", e.getMessage());
                    Thread.sleep(1000);
                } else {
                    throw e;
                }
            }
        }
    }
}
