package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.entity.ChatMessage;
import com.al.aichat.entity.ChatSession;
import com.al.aichat.mapper.ChatMessageMapper;
import com.al.aichat.mapper.SessionMapper;
import com.al.aichat.service.DeepSeekService;
import com.al.aichat.service.DeepSeekStreamService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 聊天控制器
 * 提供与 AI 对话的 RESTful 接口
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DeepSeekService deepSeekService;
    private final ChatMessageMapper chatMessageMapper;
    private final SessionMapper sessionMapper;
    private final DeepSeekStreamService deepSeekStreamService;
    private final com.al.aichat.service.KnowledgeService knowledgeService;

    public ChatController(DeepSeekService deepSeekService, ChatMessageMapper chatMessageMapper,
                          SessionMapper sessionMapper, DeepSeekStreamService deepSeekStreamService,
                          com.al.aichat.service.KnowledgeService knowledgeService) {
        this.deepSeekService = deepSeekService;
        this.chatMessageMapper = chatMessageMapper;
        this.sessionMapper = sessionMapper;
        this.deepSeekStreamService = deepSeekStreamService;
        this.knowledgeService = knowledgeService;
    }

    /**
     * 发送消息给 AI
     * <p>
     * 请求头需要携带 JWT token（格式：Authorization: Bearer <token>）
     * 请求体需要包含消息内容和会话ID：
     * {"content": "你好", "sessionId": 1}
     *
     * @param requestBody 请求体，包含 content（消息内容）和 sessionId（会话ID）
     * @param request     HTTP 请求对象，用于获取 token
     * @return AI 的回复内容
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> sendMessage(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        // 获取用户消息内容
        String content = (String) requestBody.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.error(400, "消息内容不能为空");
        }

        // 获取会话ID
        Long sessionId = null;
        Object sessionIdObj = requestBody.get("sessionId");
        if (sessionIdObj instanceof Number) {
            sessionId = ((Number) sessionIdObj).longValue();
        }

        // 调用 DeepSeek 服务获取回复（业务异常由 GlobalExceptionHandler 统一处理）
        String reply = deepSeekService.chat(userId, sessionId, content);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", reply);
        result.put("sessionId", sessionId);
        return Result.success(result);
    }

    /**
     * 流式发送消息给 AI（SSE 逐字输出）
     * <p>
     * 请求头需要携带 JWT token（格式：Authorization: Bearer <token>）
     * 请求参数：
     * - content: 消息内容
     * - sessionId: 会话 ID
     * <p>
     * 返回 SSE 事件流：
     * - event: message → data: "单字片段"
     * - event: done   → data: ""（表示结束）
     * - event: error  → data: "错误信息"
     *
     * @param content   消息内容（请求参数）
     * @param sessionId 会话 ID（请求参数）
     * @param request   HTTP 请求对象
     * @return SSE Emitter
     */
    @GetMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @RequestParam("content") String content,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        if (content == null || content.trim().isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("消息内容不能为空"));
                emitter.complete();
            } catch (IOException e) {
                // ignore
            }
            return emitter;
        }

        return deepSeekStreamService.streamChat(userId, sessionId, content);
    }

    /**
     * 获取用户的聊天历史记录（按会话分组）
     * <p>
     * 返回该用户所有有聊天记录的会话列表，按最后活动时间倒序排列。
     * 每个会话包含：sessionId、标题、消息数量、最后活动时间。
     *
     * @param request HTTP 请求对象，用于获取 token
     * @return 按会话分组的聊天历史
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> getHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 查询该用户所有有聊天记录的会话（按最后消息时间倒序）
        LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessage::getUserId, userId)
                .isNotNull(ChatMessage::getSessionId)  // 只查有 sessionId 的消息（兼容旧数据）
                .orderByDesc(ChatMessage::getCreatedAt);
        List<ChatMessage> allMessages = chatMessageMapper.selectList(msgWrapper);

        // 按 sessionId 分组
        Map<Long, List<ChatMessage>> grouped = allMessages.stream()
                .collect(Collectors.groupingBy(
                        ChatMessage::getSessionId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 构建返回结果
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Map.Entry<Long, List<ChatMessage>> entry : grouped.entrySet()) {
            Long sid = entry.getKey();
            List<ChatMessage> msgs = entry.getValue();

            Map<String, Object> session = new LinkedHashMap<>();
            session.put("sessionId", sid);
            session.put("count", msgs.size());

            // 最后一条消息的时间
            ChatMessage latestMsg = msgs.get(0);
            session.put("lastTime", latestMsg.getCreatedAt().toString());

            // 从 session 表获取标题
            ChatSession chatSession = sessionMapper.selectById(sid);
            String title = (chatSession != null && chatSession.getTitle() != null
                    && !"新对话".equals(chatSession.getTitle()))
                    ? chatSession.getTitle()
                    : "对话记录";
            session.put("title", title);

            sessions.add(session);
        }

        // 按最后活动时间倒序排列（最新的在前面）
        sessions.sort((a, b) -> ((String) b.get("lastTime")).compareTo((String) a.get("lastTime")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessions", sessions);
        result.put("total", allMessages.size());

        return Result.success(result);
    }

    /**
     * 获取某个会话的详细聊天记录
     *
     * @param sessionId 会话ID
     * @param request   HTTP 请求对象
     * @return 该会话的所有消息
     */
    @GetMapping("/history/session/{sessionId}")
    public Result<Map<String, Object>> getSessionHistory(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 查询该会话的消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getUserId, userId)
                .orderByAsc(ChatMessage::getCreatedAt);
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);

        // 获取会话标题
        ChatSession chatSession = sessionMapper.selectById(sessionId);
        String title = (chatSession != null) ? chatSession.getTitle() : "对话记录";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("title", title);
        result.put("messages", messages);
        result.put("count", messages.size());

        return Result.success(result);
    }

    /**
     * 搜索聊天记录
     *
     * GET /api/chat/search?keyword=xxx&sessionId=1&page=1&size=10
     *
     * @param keyword   搜索关键字（必填，匹配 content 字段）
     * @param sessionId 会话ID（可选，不传则搜索所有会话）
     * @param page      页码（从1开始，默认1）
     * @param size      每页条数（默认10，最大50）
     * @param request   HTTP 请求
     * @return 分页搜索结果
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error(400, "搜索关键字不能为空");
        }
        if (size > 50) {
            size = 50;
        }

        // 构建查询条件
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getUserId, userId)
                .like(ChatMessage::getContent, keyword.trim())
                .orderByDesc(ChatMessage::getCreatedAt);

        if (sessionId != null) {
            wrapper.eq(ChatMessage::getSessionId, sessionId);
        }

        // 分页查询
        Page<ChatMessage> pageObj = new Page<>(page, size);
        Page<ChatMessage> resultPage = chatMessageMapper.selectPage(pageObj, wrapper);

        // 组装返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("page", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        result.put("pages", resultPage.getPages());

        return Result.success(result);
    }

    /**
     * 导出会话聊天记录
     *
     * GET /api/chat/export/{sessionId}?format=md
     * GET /api/chat/export/{sessionId}?format=txt
     *
     * @param sessionId 会话ID
     * @param format    导出格式（md 或 txt，默认 md）
     * @param request   HTTP 请求
     * @return 文件下载
     */
    @GetMapping("/export/{sessionId}")
    public ResponseEntity<byte[]> exportSession(
            @PathVariable Long sessionId,
            @RequestParam(value = "format", defaultValue = "md") String format,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        // 校验会话归属
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        if (!session.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        // 查询该会话所有消息（按时间正序）
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getUserId, userId)
                .orderByAsc(ChatMessage::getCreatedAt);
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);

        // 生成内容
        String content;
        String filename;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if ("txt".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("会话：").append(session.getTitle()).append("\n");
            sb.append("导出时间：").append(LocalDateTime.now().format(dtf)).append("\n");
            sb.append("=" .repeat(50)).append("\n\n");
            for (ChatMessage msg : messages) {
                String role = "user".equals(msg.getRole()) ? "你" : "AI";
                String time = msg.getCreatedAt() != null ? msg.getCreatedAt().format(dtf) : "";
                sb.append("【").append(role).append("】").append(" ").append(time).append("\n");
                sb.append(msg.getContent()).append("\n\n");
            }
            content = sb.toString();
            filename = sanitizeFilename(session.getTitle()) + ".txt";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(session.getTitle()).append("\n\n");
            sb.append("> 导出时间：").append(LocalDateTime.now().format(dtf)).append("\n\n");
            sb.append("---\n\n");
            for (ChatMessage msg : messages) {
                String role = "user".equals(msg.getRole()) ? "**你**" : "**AI**";
                String time = msg.getCreatedAt() != null ? msg.getCreatedAt().format(dtf) : "";
                sb.append(role).append("  \n");
                sb.append("*").append(time).append("*\n\n");
                sb.append(msg.getContent()).append("\n\n");
                sb.append("---\n\n");
            }
            content = sb.toString();
            filename = sanitizeFilename(session.getTitle()) + ".md";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok().headers(headers).body(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * RAG 增强问答 — 先检索知识库，再调用 DeepSeek
     *
     * POST /api/chat/rag
     * Body: {"question":"xxx", "sessionId":1}
     */
    @PostMapping("/rag")
    public Result<Map<String, Object>> ragChat(@RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String question = (String) body.get("question");
        if (question == null || question.isBlank()) return Result.error(400, "问题不能为空");

        Long sessionId = null;
        Object sid = body.get("sessionId");
        if (sid instanceof Number) sessionId = ((Number) sid).longValue();

        // 检索知识库
        var chunks = knowledgeService.search(userId, question, 5);
        StringBuilder context = new StringBuilder();
        if (!chunks.isEmpty()) {
            context.append("以下是用户知识库中与问题相关的参考内容：\n\n");
            for (var c : chunks) {
                context.append("【来源：").append(c.getFilename()).append("】\n");
                context.append(c.getContent()).append("\n\n");
            }
        }

        // 构建增强提示词 — 把知识库内容和问题放在一起
        String prompt;
        if (!context.isEmpty()) {
            prompt = "知识库内容：\n" + context + "\n问题：" + question;
        } else {
            prompt = question;
        }

        // 调用 RAG 专用方法（有知识库感知的系统提示词）
        String reply = deepSeekService.ragChat(userId, sessionId, prompt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", reply);
        result.put("sessionId", sessionId);
        result.put("chunksUsed", chunks.size());
        return Result.success(result);
    }

    /** 清理文件名中的非法字符 */
    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "chat";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
