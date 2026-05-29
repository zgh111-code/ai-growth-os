package com.al.aichat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String filename;
    private String title;
    private String content;
    private Integer chunkIndex;
    private Integer charCount;

    /** 向量 embedding（1536维），仅供内部检索使用 */
    @JsonIgnore
    private float[] embedding;

    /** 检索相似度分数（仅查询结果使用，不入库） */
    @TableField(exist = false)
    private Double similarity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
