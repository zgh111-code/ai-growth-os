package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.entity.ChatSession;
import com.al.aichat.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话 Controller
 *
 * 提供会话的增删改查接口。
 * 所有接口都需要登录（通过 JWT 验证用户身份）。
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * POST /api/session/create
     *
     * 创建新会话
     */
    @PostMapping("/create")
    public Result<ChatSession> createSession(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ChatSession session = sessionService.createSession(userId);
        return Result.success(session);
    }

    /**
     * GET /api/session/list
     *
     * 获取当前用户的所有会话列表
     */
    @GetMapping("/list")
    public Result<List<ChatSession>> listSessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ChatSession> sessions = sessionService.getUserSessions(userId);
        return Result.success(sessions);
    }

    /**
     * DELETE /api/session/delete/{sessionId}
     *
     * 删除指定会话
     */
    @DeleteMapping("/delete/{sessionId}")
    public Result<String> deleteSession(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        sessionService.deleteSession(sessionId, userId);
        return Result.success("删除成功");
    }

    /**
     * POST /api/session/clean
     *
     * 清理该用户所有会话及聊天记录
     */
    @PostMapping("/clean")
    public Result<Integer> cleanEmptySessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = sessionService.cleanEmptySessions(userId);
        return Result.success("已清理 " + count + " 个会话", count);
    }

}
