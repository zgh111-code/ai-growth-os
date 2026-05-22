package com.al.aichat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project_record")
public class ProjectRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String projectName;
    private String status;
    private String blocker;
    private String progress;
    private String aiCurrentFocus;
    private String aiNextTasks;
    private String aiRisks;
    private String aiAdvice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
