-- ============================================
-- 知识库分块表（RAG 功能）
-- ============================================
CREATE TABLE IF NOT EXISTS `knowledge_chunk` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分块ID',
    `user_id`     BIGINT       NOT NULL COMMENT '上传用户ID',
    `filename`    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `title`       VARCHAR(255) DEFAULT NULL COMMENT '文档标题（从内容第一行提取）',
    `content`     TEXT         NOT NULL COMMENT '分块文本内容',
    `chunk_index` INT          NOT NULL DEFAULT 0 COMMENT '分块序号',
    `char_count`  INT          NOT NULL DEFAULT 0 COMMENT '字符数',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_filename` (`filename`(100)),
    FULLTEXT KEY `ft_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分块表';
