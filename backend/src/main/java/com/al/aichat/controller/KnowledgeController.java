package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.entity.KnowledgeChunk;
import com.al.aichat.service.KnowledgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /** POST /api/knowledge/upload — 上传文档 */
    @PostMapping("/upload")
    public Result<List<KnowledgeChunk>> upload(@RequestParam("file") MultipartFile file,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<KnowledgeChunk> chunks = knowledgeService.upload(userId, file);
        return Result.success("已解析为 " + chunks.size() + " 个分块", chunks);
    }

    /** GET /api/knowledge/files — 文件列表 */
    @GetMapping("/files")
    public Result<List<Map<String, Object>>> listFiles(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(knowledgeService.listFiles(userId));
    }

    /** DELETE /api/knowledge/delete?filename=xxx — 删除文件 */
    @DeleteMapping("/delete")
    public Result<Integer> deleteFile(@RequestParam("filename") String filename,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = knowledgeService.deleteFile(userId, filename);
        return Result.success("已删除 " + count + " 个分块", count);
    }
}
