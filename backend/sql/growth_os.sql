-- ============================================
-- AI Growth OS - 个人成长操作系统 数据库表
-- ============================================

USE ai_chat;

-- ============================================
-- 模块1：每日复盘表
-- ============================================
CREATE TABLE IF NOT EXISTS `daily_review` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `content`       TEXT         NOT NULL COMMENT '用户原始复盘内容',
    `mood`          VARCHAR(20)  DEFAULT NULL COMMENT '情绪状态：happy/neutral/sad/anxious',
    `ai_summary`    TEXT         DEFAULT NULL COMMENT 'AI生成的今日总结',
    `ai_positives`  JSON         DEFAULT NULL COMMENT 'AI：做得好的地方 [string]',
    `ai_problems`   JSON         DEFAULT NULL COMMENT 'AI：问题分析 [string]',
    `ai_suggestions` JSON        DEFAULT NULL COMMENT 'AI：明日建议 [string]',
    `review_date`   DATE         NOT NULL COMMENT '复盘日期',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `review_date`) COMMENT '按用户+日期查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日复盘表';

-- ============================================
-- 模块2：学习追踪表
-- ============================================
CREATE TABLE IF NOT EXISTS `learning_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `category`      VARCHAR(20)  NOT NULL COMMENT '类别：english/coding/project/exercise',
    `duration_min`  INT          DEFAULT 0 COMMENT '学习时长（分钟）',
    `note`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `record_date`   DATE         NOT NULL COMMENT '打卡日期',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date_cat` (`user_id`, `record_date`, `category`) COMMENT '每天每类别只能打卡一次',
    KEY `idx_user_date` (`user_id`, `record_date`) COMMENT '按用户+日期查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习追踪表';

-- ============================================
-- 模块3：表达训练表
-- ============================================
CREATE TABLE IF NOT EXISTS `expression_record` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`               BIGINT       NOT NULL COMMENT '用户ID',
    `topic`                 VARCHAR(200) NOT NULL COMMENT '表达主题',
    `content`               TEXT         NOT NULL COMMENT '用户表达内容',
    `ai_logic_score`        INT          DEFAULT NULL COMMENT 'AI：逻辑评分 0-100',
    `ai_clarity_score`      INT          DEFAULT NULL COMMENT 'AI：清晰度评分 0-100',
    `ai_problems`           JSON         DEFAULT NULL COMMENT 'AI：表达问题 [string]',
    `ai_optimized_expression` TEXT       DEFAULT NULL COMMENT 'AI：优化后的表达示例',
    `ai_suggestions`        JSON         DEFAULT NULL COMMENT 'AI：训练建议 [string]',
    `created_at`            DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '按用户查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表达训练表';

-- ============================================
-- 模块4：项目推进表
-- ============================================
CREATE TABLE IF NOT EXISTS `project_record` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT       NOT NULL COMMENT '用户ID',
    `project_name`      VARCHAR(100) NOT NULL COMMENT '项目名称',
    `status`            TEXT         NOT NULL COMMENT '当前状态描述',
    `blocker`           TEXT         DEFAULT NULL COMMENT '卡点',
    `progress`          TEXT         DEFAULT NULL COMMENT '今日进度',
    `ai_current_focus`  TEXT         DEFAULT NULL COMMENT 'AI：当前最应该做的事',
    `ai_next_tasks`     JSON         DEFAULT NULL COMMENT 'AI：下一步任务 [{task,priority}]',
    `ai_risks`          JSON         DEFAULT NULL COMMENT 'AI：风险提醒 [string]',
    `ai_advice`         JSON         DEFAULT NULL COMMENT 'AI：执行建议 [string]',
    `created_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '按用户查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目推进表';
