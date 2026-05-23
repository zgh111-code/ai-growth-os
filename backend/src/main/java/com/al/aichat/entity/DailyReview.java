package com.al.aichat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_review")
public class DailyReview {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String content;
    private String mood;
    private String aiSummary;
    private String aiPositives;   // JSON array string
    private String aiProblems;    // JSON array string
    private String aiSuggestions; // JSON array string
    private LocalDate reviewDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
