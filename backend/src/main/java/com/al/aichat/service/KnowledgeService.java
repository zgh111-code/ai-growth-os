package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.entity.KnowledgeChunk;
import com.al.aichat.mapper.KnowledgeChunkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 100;

    public KnowledgeService(KnowledgeChunkMapper chunkMapper, EmbeddingService embeddingService) {
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
    }

    /**
     * 上传并解析文档，生成 embedding 向量
     */
    public List<KnowledgeChunk> upload(Long userId, MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("文件不能为空");
        if (file.getSize() > 5 * 1024 * 1024) throw new BusinessException("文件不能超过 5MB");

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(txt|md|java|py|js|html|css|json|xml|yml|sql)$")) {
            throw new BusinessException("仅支持 TXT/MD/代码文件");
        }

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

        // 提取标题
        String title = filename;
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim().replaceAll("^[#*\\- ]+", "").trim();
            if (!trimmed.isEmpty()) { title = trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed; break; }
        }

        // 分块（带重叠）
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
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

                // 生成 embedding（失败不阻塞上传）
                try {
                    float[] vector = embeddingService.embed(chunk);
                    KnowledgeChunk update = new KnowledgeChunk();
                    update.setId(kc.getId());
                    update.setEmbedding(vector);
                    chunkMapper.updateById(update);
                    kc.setEmbedding(vector);
                } catch (Exception e) {
                    log.warn("分块 {} embedding 失败: {}", kc.getChunkIndex(), e.getMessage());
                }

                chunks.add(kc);
            }
            start = end + 1 - OVERLAP;
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
               .eq(KnowledgeChunk::getChunkIndex, 0)
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
     * 混合检索：语义向量 + 关键词加权融合
     *
     * 1. 查询 embedding → pgvector 余弦相似度召回 topK*2 候选
     * 2. 对候选计算关键词匹配分数
     * 3. 加权融合: finalScore = 0.7 * semanticScore + 0.3 * keywordScore
     * 4. 取 topK 返回
     */
    public List<KnowledgeChunk> search(Long userId, String query, int topK) {
        // Step 1: 语义检索
        float[] queryVector = embeddingService.embed(query);
        String vectorStr = vectorToString(queryVector);
        List<KnowledgeChunk> semanticResults = chunkMapper.searchByEmbedding(userId, vectorStr, topK * 2);

        if (semanticResults.isEmpty()) {
            return List.of();
        }

        // Step 2: 混合排序
        for (KnowledgeChunk chunk : semanticResults) {
            double keywordScore = computeKeywordScore(chunk.getContent(), query);
            double semanticScore = chunk.getSimilarity() != null ? chunk.getSimilarity() : 0.0;
            chunk.setSimilarity(0.7 * semanticScore + 0.3 * keywordScore);
        }

        // Step 3: 按混合分数排序
        semanticResults.sort((a, b) -> Double.compare(
                b.getSimilarity() != null ? b.getSimilarity() : 0.0,
                a.getSimilarity() != null ? a.getSimilarity() : 0.0));

        return semanticResults.stream().limit(topK).collect(Collectors.toList());
    }

    // ---- 私有工具方法 ----

    private int countChunks(Long userId, String filename) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getUserId, userId)
               .eq(KnowledgeChunk::getFilename, filename);
        return Math.toIntExact(chunkMapper.selectCount(wrapper));
    }

    /** float[] → pgvector 字符串格式: [0.1,0.2,...] */
    private String vectorToString(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /** 计算关键词匹配分数（0~1），修复了旧 countMatches 的大小写敏感 bug */
    private double computeKeywordScore(String text, String query) {
        if (text == null || query == null || query.isBlank()) return 0.0;
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        // 完全匹配
        if (lowerText.contains(lowerQuery)) return 1.0;

        // 空格分隔的词匹配（权重 60%）
        String[] words = lowerQuery.split("\\s+");
        int totalWords = words.length;
        int matchedWords = 0;
        for (String word : words) {
            if (word.length() >= 1 && lowerText.contains(word)) matchedWords++;
        }
        double wordScore = totalWords > 0 ? (double) matchedWords / totalWords : 0.0;

        // 双字滑动窗口（权重 40%，对中文有效）
        int totalBigrams = Math.max(1, lowerQuery.length() - 1);
        int matchedBigrams = 0;
        for (int i = 0; i < lowerQuery.length() - 1; i++) {
            if (lowerText.contains(lowerQuery.substring(i, i + 2))) matchedBigrams++;
        }
        double bigramScore = (double) matchedBigrams / totalBigrams;

        return 0.6 * wordScore + 0.4 * bigramScore;
    }
}
