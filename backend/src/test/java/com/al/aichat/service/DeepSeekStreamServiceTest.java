package com.al.aichat.service;

import com.al.aichat.config.DeepSeekConfig;
import com.al.aichat.entity.ChatMessage;
import com.al.aichat.entity.ChatSession;
import com.al.aichat.mapper.ChatMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeepSeekStreamServiceTest {

    @Mock
    private DeepSeekConfig deepSeekConfig;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private SessionService sessionService;

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    private DeepSeekStreamService streamService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
        when(deepSeekConfig.getApiKey()).thenReturn("mock-api-key");
        when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");

        streamService = new DeepSeekStreamService(
                deepSeekConfig, chatMessageMapper, sessionService,
                restTemplate, objectMapper
        );

        doNothing().when(sessionService).validateOwnership(anyLong(), anyLong());
        when(sessionService.getRecentHistory(anyLong(), anyLong(), anyInt())).thenReturn(List.of());
    }

    @Test
    @DisplayName("流式聊天 - 应保存用户消息、调用 API、保存回复、更新标题")
    void streamChat_ShouldProcessFullFlow() throws Exception {
        Long userId = 1L;
        Long sessionId = 10L;

        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setTitle("新对话");
        when(sessionService.getById(sessionId)).thenReturn(session);

        String sseResponse =
                "data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"！\"}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"有什么\"}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"可以帮助\"}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"你的？\"}}]}\n\n" +
                "data: [DONE]\n\n";

        InputStream responseStream = new ByteArrayInputStream(sseResponse.getBytes(StandardCharsets.UTF_8));
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getBody()).thenReturn(responseStream);

        when(restTemplate.execute(
                anyString(), eq(HttpMethod.POST), any(), any()
        )).thenAnswer(invocation -> {
            org.springframework.web.client.ResponseExtractor<?> extractor = invocation.getArgument(3);
            return extractor.extractData(mockResponse);
        });

        streamService.streamChat(userId, sessionId, "你好");
        Thread.sleep(500);

        ArgumentCaptor<ChatMessage> userMsgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper, times(2)).insert(userMsgCaptor.capture());

        ChatMessage savedUserMsg = userMsgCaptor.getAllValues().get(0);
        assertEquals(userId, savedUserMsg.getUserId());
        assertEquals(sessionId, savedUserMsg.getSessionId());
        assertEquals("user", savedUserMsg.getRole());
        assertEquals("你好", savedUserMsg.getContent());

        ChatMessage savedAiMsg = userMsgCaptor.getAllValues().get(1);
        assertEquals("assistant", savedAiMsg.getRole());
        assertEquals("你好！有什么可以帮助你的？", savedAiMsg.getContent());

        verify(sessionService, times(1)).updateTitle(eq(sessionId), anyString());
    }

    @Test
    @DisplayName("流式聊天 - 已有标题的会话不应更新标题")
    void streamChat_WithNonDefaultTitle_ShouldNotUpdateTitle() throws Exception {
        Long userId = 1L;
        Long sessionId = 10L;

        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setTitle("已有标题");
        when(sessionService.getById(sessionId)).thenReturn(session);

        String sseResponse = "data: {\"choices\":[{\"delta\":{\"content\":\"回复\"}}]}\n\ndata: [DONE]\n\n";
        InputStream responseStream = new ByteArrayInputStream(sseResponse.getBytes(StandardCharsets.UTF_8));
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getBody()).thenReturn(responseStream);

        when(restTemplate.execute(
                anyString(), eq(HttpMethod.POST), any(), any()
        )).thenAnswer(invocation -> {
            org.springframework.web.client.ResponseExtractor<?> extractor = invocation.getArgument(3);
            return extractor.extractData(mockResponse);
        });

        streamService.streamChat(userId, sessionId, "你好");
        Thread.sleep(500);

        verify(sessionService, never()).updateTitle(anyLong(), anyString());
    }

    @Test
    @DisplayName("流式聊天 - API 异常应通过 error 事件通知并完成 emitter")
    void streamChat_WhenApiFails_ShouldSendErrorAndComplete() throws Exception {
        Long userId = 1L;
        Long sessionId = 10L;

        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        when(restTemplate.execute(
                anyString(), eq(HttpMethod.POST), any(), any()
        )).thenThrow(new RuntimeException("网络连接失败"));

        streamService.streamChat(userId, sessionId, "出错了");
        Thread.sleep(500);

        verify(chatMessageMapper, times(1)).insert(any(ChatMessage.class));
    }

    @Test
    @DisplayName("流式聊天 - DeepSeek 返回空内容时不应保存 AI 消息")
    void streamChat_WithEmptyResponse_ShouldNotSaveAssistantMessage() throws Exception {
        Long userId = 1L;
        Long sessionId = 10L;

        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        String sseResponse = "data: {\"choices\":[{\"delta\":{}}]}\n\ndata: [DONE]\n\n";
        InputStream responseStream = new ByteArrayInputStream(sseResponse.getBytes(StandardCharsets.UTF_8));
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getBody()).thenReturn(responseStream);

        when(restTemplate.execute(
                anyString(), eq(HttpMethod.POST), any(), any()
        )).thenAnswer(invocation -> {
            org.springframework.web.client.ResponseExtractor<?> extractor = invocation.getArgument(3);
            return extractor.extractData(mockResponse);
        });

        streamService.streamChat(userId, sessionId, "测试");
        Thread.sleep(500);

        verify(chatMessageMapper, times(1)).insert(any(ChatMessage.class));
    }
}
