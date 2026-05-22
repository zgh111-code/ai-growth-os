package com.al.aichat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("expression_record")
public class ExpressionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String topic;
    private String content;
    private Integer aiLogicScore;
    private Integer aiClarityScore;
    private String aiProblems;
    private String aiOptimizedExpression;
    private String aiSuggestions;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
