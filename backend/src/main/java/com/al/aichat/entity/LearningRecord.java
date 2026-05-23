package com.al.aichat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("learning_record")
public class LearningRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String category;   // english/coding/project/exercise
    private Integer durationMin;
    private String note;
    private LocalDate recordDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
