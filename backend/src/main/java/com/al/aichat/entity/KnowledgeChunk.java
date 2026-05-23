package com.al.aichat.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
