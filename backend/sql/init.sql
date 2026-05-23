-- ============================================
-- AI 聊天平台 - 数据库初始化脚本
-- ============================================

-- 创建数据库（如果不存在）
-- 使用 utf8mb4 编码，支持中文和 emoji 表情
CREATE DATABASE IF NOT EXISTS ai_chat
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE ai_chat;

-- ============================================
-- 用户表
-- 存储用户的注册信息和登录凭证
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键，自增）',
    `username`   VARCHAR(50)  NOT NULL COMMENT '用户名（登录用）',
    `password`   VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称（显示用）',
    `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `email`      VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `status`     TINYINT      DEFAULT 1   COMMENT '状态：0-禁用，1-正常',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 聊天会话表
-- 存储用户的聊天会话，每个会话包含多条消息
-- ============================================
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID（主键，自增）',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID（关联 user 表）',
    `title`       VARCHAR(100) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '按用户ID查询会话的索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- ============================================
-- 聊天消息表
-- 存储用户和 AI 的对话记录，每条消息属于一个会话
-- ============================================
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID（主键，自增）',
    `session_id`  BIGINT       NOT NULL COMMENT '会话ID（关联 chat_session 表）',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID（关联 user 表）',
    `role`        VARCHAR(20)  NOT NULL COMMENT '角色：user-用户，assistant-AI',
    `content`     TEXT         NOT NULL COMMENT '消息内容',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`) COMMENT '按会话ID查询消息的索引',
    KEY `idx_user_id` (`user_id`) COMMENT '按用户ID查询的索引',
    KEY `idx_created_at` (`created_at`) COMMENT '按时间排序的索引',
    KEY `idx_user_session_time` (`user_id`, `session_id`, `created_at`) COMMENT '联合索引：按用户+会话+时间高效查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';
