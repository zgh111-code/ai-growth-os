package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.entity.ChatMessage;
import com.al.aichat.entity.ChatSession;
import com.al.aichat.mapper.ChatMessageMapper;
import com.al.aichat.mapper.SessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SessionService {

    private final SessionMapper sessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    public SessionService(SessionMapper sessionMapper, ChatMessageMapper chatMessageMapper) {
        this.sessionMapper = sessionMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    public ChatSession createSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle("新对话");
        sessionMapper.insert(session);
        return session;
    }

    public List<ChatSession> getUserSessions(Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
               .orderByDesc(ChatSession::getCreatedAt);
        return sessionMapper.selectList(wrapper);
    }

    public void updateTitle(Long sessionId, String title) {
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setTitle(title);
        sessionMapper.updateById(session);
    }

    public void deleteSession(Long sessionId, Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getId, sessionId)
               .eq(ChatSession::getUserId, userId);
        sessionMapper.delete(wrapper);
    }

    public ChatSession getById(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    public void validateOwnership(Long sessionId, Long userId) {
        if (sessionId == null) {
            return;
        }
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该会话");
        }
    }

    public int cleanEmptySessions(Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
               .eq(ChatSession::getTitle, "新对话");
        List<ChatSession> emptySessions = sessionMapper.selectList(wrapper);
        int count = 0;
        for (ChatSession session : emptySessions) {
            Long msgCount = chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, session.getId())
            );
            if (msgCount == null || msgCount == 0) {
                sessionMapper.deleteById(session.getId());
                count++;
            }
        }
        return count;
    }

    /**
     * 获取指定会话最近的聊天历史（供 DeepSeek 服务构建上下文使用）
     */
    public List<ChatMessage> getRecentHistory(Long userId, Long sessionId, int limit) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT " + limit);
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        Collections.reverse(messages);
        return messages;
    }
}
