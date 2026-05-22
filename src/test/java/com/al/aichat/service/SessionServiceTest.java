package com.al.aichat.service;

import com.al.aichat.entity.ChatMessage;
import com.al.aichat.entity.ChatSession;
import com.al.aichat.mapper.ChatMessageMapper;
import com.al.aichat.mapper.SessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SessionService 单元测试
 *
 * 测试会话的 CRUD 业务逻辑。
 * 使用 Mockito 模拟 SessionMapper 和 ChatMessageMapper，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionMapper sessionMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionMapper, chatMessageMapper);
    }

    // ==================== 创建会话测试 ====================

    @Test
    @DisplayName("创建会话 - 应成功创建并返回含默认标题的会话")
    void createSession_ShouldReturnSessionWithDefaultTitle() {
        // 模拟：insert 成功后会将 id 回写到实体
        doAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(1L);
            return 1;
        }).when(sessionMapper).insert(any(ChatSession.class));

        // 执行
        ChatSession result = sessionService.createSession(100L);

        // 验证
        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertEquals("新对话", result.getTitle());
        assertEquals(1L, result.getId());

        verify(sessionMapper, times(1)).insert(any(ChatSession.class));
    }

    // ==================== 查询会话列表测试 ====================

    @Test
    @DisplayName("获取会话列表 - 应按时间倒序返回用户的会话")
    void getUserSessions_ShouldReturnSessionsOrderedByTimeDesc() {
        // 准备
        ChatSession session1 = new ChatSession();
        session1.setId(1L);
        session1.setUserId(100L);
        session1.setTitle("第一个会话");
        session1.setCreatedAt(LocalDateTime.now().minusDays(1));

        ChatSession session2 = new ChatSession();
        session2.setId(2L);
        session2.setUserId(100L);
        session2.setTitle("第二个会话");
        session2.setCreatedAt(LocalDateTime.now());

        when(sessionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(session2, session1));

        // 执行
        List<ChatSession> sessions = sessionService.getUserSessions(100L);

        // 验证
        assertEquals(2, sessions.size());
        assertEquals("第二个会话", sessions.get(0).getTitle());
        assertEquals("第一个会话", sessions.get(1).getTitle());

        verify(sessionMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取会话列表 - 没有会话应返回空列表")
    void getUserSessions_WithNoSessions_ShouldReturnEmptyList() {
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<ChatSession> sessions = sessionService.getUserSessions(100L);

        assertTrue(sessions.isEmpty());
    }

    // ==================== 更新标题测试 ====================

    @Test
    @DisplayName("更新标题 - 应成功更新")
    void updateTitle_ShouldUpdateSessionTitle() {
        // 执行
        sessionService.updateTitle(1L, "新标题");

        // 验证：updateById 被调用，且传入了正确的 id 和 title
        verify(sessionMapper, times(1)).updateById(argThat(session ->
                session.getId().equals(1L) && "新标题".equals(session.getTitle())
        ));
    }

    // ==================== 删除会话测试 ====================

    @Test
    @DisplayName("删除会话 - 用户有权限时应成功删除")
    void deleteSession_WithCorrectUserId_ShouldDelete() {
        // 模拟
        when(sessionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // 执行
        sessionService.deleteSession(1L, 100L);

        // 验证：delete 被调用一次
        verify(sessionMapper, times(1)).delete(any(LambdaQueryWrapper.class));
    }

    // ==================== 根据ID获取会话测试 ====================

    @Test
    @DisplayName("根据ID获取会话 - 会话存在应返回会话")
    void getById_WithExistingSession_ShouldReturnSession() {
        // 准备
        ChatSession session = new ChatSession();
        session.setId(1L);
        session.setUserId(100L);
        session.setTitle("测试会话");

        when(sessionMapper.selectById(1L)).thenReturn(session);

        // 执行
        ChatSession result = sessionService.getById(1L);

        // 验证
        assertNotNull(result);
        assertEquals("测试会话", result.getTitle());
        assertEquals(100L, result.getUserId());
    }

    @Test
    @DisplayName("根据ID获取会话 - 不存在应返回 null")
    void getById_WithNonExistentSession_ShouldReturnNull() {
        when(sessionMapper.selectById(999L)).thenReturn(null);

        ChatSession result = sessionService.getById(999L);

        assertNull(result);
    }

    // ==================== 清理空会话测试 ====================

    @Test
    @DisplayName("清理空会话 - 应删除没有消息记录的会话")
    void cleanEmptySessions_ShouldDeleteEmptyOnes() {
        // 准备：有两个标题为"新对话"的会话
        ChatSession emptySession1 = new ChatSession();
        emptySession1.setId(1L);
        emptySession1.setUserId(100L);
        emptySession1.setTitle("新对话");

        ChatSession emptySession2 = new ChatSession();
        emptySession2.setId(2L);
        emptySession2.setUserId(100L);
        emptySession2.setTitle("新对话");

        when(sessionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(emptySession1, emptySession2));

        // 两个会话都没有消息记录
        when(chatMessageMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L);

        // 执行
        int count = sessionService.cleanEmptySessions(100L);

        // 验证：两个会话都被删除
        assertEquals(2, count);

        // 验证 deleteById 被调用了两次
        verify(sessionMapper, times(2)).deleteById(anyLong());
    }

    @Test
    @DisplayName("清理空会话 - 有消息的会话不应被删除")
    void cleanEmptySessions_WithMessage_ShouldNotDelete() {
        // 准备
        ChatSession sessionWithMessages = new ChatSession();
        sessionWithMessages.setId(1L);
        sessionWithMessages.setUserId(100L);
        sessionWithMessages.setTitle("新对话");

        when(sessionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(sessionWithMessages));

        // 该会话有消息记录
        when(chatMessageMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(3L);

        // 执行
        int count = sessionService.cleanEmptySessions(100L);

        // 验证：没有被删除
        assertEquals(0, count);
        verify(sessionMapper, never()).deleteById(anyLong());
    }
}