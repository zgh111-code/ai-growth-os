package com.al.aichat.mapper;

import com.al.aichat.entity.KnowledgeChunk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    /**
     * pgvector 余弦相似度检索
     * embedding <=> query_vector 返回余弦距离，值越小越相似
     * 1 - cosine_distance = 余弦相似度
     */
    @Select("SELECT *, (1 - (embedding <=> #{embedding}::vector)) AS similarity " +
            "FROM knowledge_chunk " +
            "WHERE user_id = #{userId} " +
            "  AND embedding IS NOT NULL " +
            "ORDER BY embedding <=> #{embedding}::vector " +
            "LIMIT #{topK}")
    List<KnowledgeChunk> searchByEmbedding(@Param("userId") Long userId,
                                           @Param("embedding") String embedding,
                                           @Param("topK") int topK);
}
