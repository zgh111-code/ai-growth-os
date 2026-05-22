package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.entity.KnowledgeChunk;
import com.al.aichat.mapper.KnowledgeChunkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private final KnowledgeChunkMapper chunkMapper;
    private static final int CHUNK_SIZE = 500; // 每块最多 500 字符

    public KnowledgeService(KnowledgeChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    /**
     * 上传并解析文档
     */
    public List<KnowledgeChunk> upload(Long userId, MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("文件不能为空");
        if (file.getSize() > 5 * 1024 * 1024) throw new BusinessException("文件不能超过 5MB");

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(txt|md|java|py|js|html|css|json|xml|yml|sql)$")) {
            throw new BusinessException("仅支持 TXT/MD/代码文件");
        }

        // 读取文件内容
        String content;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new BusinessException("文件读取失败");
        }

        if (content.isBlank()) throw new BusinessException("文件内容为空");

        // 删除旧数据
        LambdaQueryWrapper<KnowledgeChunk> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(KnowledgeChunk::getUserId, userId)
                  .eq(KnowledgeChunk::getFilename, filename);
        chunkMapper.delete(delWrapper);

        // 提取标题（第一行非空行，去掉 # 等标记）
        String title = filename;
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim().replaceAll("^[#*\\- ]+", "").trim();
            if (!trimmed.isEmpty()) { title = trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed; break; }
        }

        // 分块
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
            // 尽量在换行处断开
            if (end < content.length()) {
                int nl = content.lastIndexOf('\n', end);
                if (nl > start + CHUNK_SIZE / 2) end = nl;
            }
            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                KnowledgeChunk kc = new KnowledgeChunk();
                kc.setUserId(userId);
                kc.setFilename(filename);
                kc.setTitle(title);
                kc.setContent(chunk);
                kc.setChunkIndex(index++);
                kc.setCharCount(chunk.length());
                chunkMapper.insert(kc);
                chunks.add(kc);
            }
            start = end + 1;
        }

        return chunks;
    }

    /**
     * 获取用户上传的文件列表
     */
    public List<Map<String, Object>> listFiles(Long userId) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(KnowledgeChunk::getFilename, KnowledgeChunk::getTitle, KnowledgeChunk::getId)
               .eq(KnowledgeChunk::getUserId, userId)
               .eq(KnowledgeChunk::getChunkIndex, 0)  // 只取第一条代表文件
               .orderByDesc(KnowledgeChunk::getCreatedAt);
        return chunkMapper.selectList(wrapper).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("filename", c.getFilename());
            m.put("title", c.getTitle());
            m.put("chunkCount", countChunks(userId, c.getFilename()));
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 删除文件的所有分块
     */
    public int deleteFile(Long userId, String filename) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getUserId, userId)
               .eq(KnowledgeChunk::getFilename, filename);
        return chunkMapper.delete(wrapper);
    }

    /**
     * 关键词检索 — 支持中英文，自动拆分短语
     */
    public List<KnowledgeChunk> search(Long userId, String keyword, int topK) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getUserId, userId);

        // 空格分隔的词直接 LIKE
        String[] words = keyword.trim().split("\\s+");
        if (words.length >= 2) {
            for (String word : words) {
                if (word.length() >= 1) wrapper.like(KnowledgeChunk::getContent, word);
            }
        } else {
            // 单个中文短语：用双字滑动窗口匹配
            String kw = words[0];
            wrapper.and(w -> {
                for (int i = 0; i < kw.length() - 1; i++) {
                    w.or().like(KnowledgeChunk::getContent, kw.substring(i, Math.min(i + 2, kw.length())));
                }
            });
        }

        wrapper.orderByAsc(KnowledgeChunk::getFilename, KnowledgeChunk::getChunkIndex);
        wrapper.last("LIMIT " + (topK * 3));

        List<KnowledgeChunk> results = chunkMapper.selectList(wrapper);
        results.sort((a, b) -> Integer.compare(
            countMatches(b.getContent(), keyword),
            countMatches(a.getContent(), keyword)
        ));
        return results.stream().limit(topK).collect(Collectors.toList());
    }

    private int countChunks(Long userId, String filename) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getUserId, userId)
               .eq(KnowledgeChunk::getFilename, filename);
        return Math.toIntExact(chunkMapper.selectCount(wrapper));
    }

    /** 计算双字匹配命中数 */
    private int countMatches(String text, String keyword) {
        int count = 0;
        String lower = text;
        for (int i = 0; i < keyword.length() - 1; i++) {
            if (lower.contains(keyword.substring(i, Math.min(i + 2, keyword.length())))) count++;
        }
        return count;
    }
}
