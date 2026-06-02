-- ============================================
-- AI Growth OS - PostgreSQL 初始化脚本
-- 适用版本: PostgreSQL 16 + pgvector 扩展
-- ============================================

-- 启用 pgvector 扩展（向量检索）
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS "user" (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(50),
    avatar      VARCHAR(255),
    email       VARCHAR(100),
    phone       VARCHAR(20),
    status      SMALLINT     DEFAULT 1,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 2. 聊天会话表
-- ============================================
CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL DEFAULT '新对话',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cs_user_id ON chat_session(user_id);

-- ============================================
-- 3. 聊天消息表
-- ============================================
CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cm_session_id ON chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_cm_user_id ON chat_message(user_id);
CREATE INDEX IF NOT EXISTS idx_cm_created_at ON chat_message(created_at);
CREATE INDEX IF NOT EXISTS idx_cm_user_session_time ON chat_message(user_id, session_id, created_at);

-- ============================================
-- 4. 知识库分块表（含向量字段）
-- ============================================
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    filename      VARCHAR(255) NOT NULL,
    title         VARCHAR(255) DEFAULT NULL,
    content       TEXT         NOT NULL,
    chunk_index   INTEGER      NOT NULL DEFAULT 0,
    char_count    INTEGER      NOT NULL DEFAULT 0,
    embedding     VECTOR(1536) DEFAULT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_kc_user_id ON knowledge_chunk(user_id);
CREATE INDEX IF NOT EXISTS idx_kc_filename ON knowledge_chunk(filename);
CREATE INDEX IF NOT EXISTS idx_kc_user_filename ON knowledge_chunk(user_id, filename);

-- 注: ivfflat 向量索引需要在表中插入一定数据后才能创建:
-- CREATE INDEX idx_kc_embedding ON knowledge_chunk
--   USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10);

-- ============================================
-- 5. 每日复盘表
-- ============================================
CREATE TABLE IF NOT EXISTS daily_review (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    content         TEXT         NOT NULL,
    mood            VARCHAR(20),
    ai_summary      TEXT,
    ai_positives    TEXT,
    ai_problems     TEXT,
    ai_suggestions  TEXT,
    review_date     DATE         NOT NULL,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dr_user_date ON daily_review(user_id, review_date);

-- ============================================
-- 6. 学习追踪表
-- ============================================
CREATE TABLE IF NOT EXISTS learning_record (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    duration_min  INTEGER      DEFAULT 0,
    note          VARCHAR(500),
    record_date   DATE         NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, record_date, category)
);
CREATE INDEX IF NOT EXISTS idx_lr_user_date ON learning_record(user_id, record_date);

-- ============================================
-- 7. 表达训练表
-- ============================================
CREATE TABLE IF NOT EXISTS expression_record (
    id                      BIGSERIAL    PRIMARY KEY,
    user_id                 BIGINT       NOT NULL,
    topic                   VARCHAR(200) NOT NULL,
    content                 TEXT,
    ai_logic_score          INTEGER,
    ai_clarity_score        INTEGER,
    ai_problems             TEXT,
    ai_optimized_expression TEXT,
    ai_suggestions          TEXT,
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_er_user_id ON expression_record(user_id);

-- ============================================
-- 8. 项目推进表
-- ============================================
CREATE TABLE IF NOT EXISTS project_record (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    project_name      VARCHAR(100) NOT NULL,
    status            TEXT         NOT NULL,
    blocker           TEXT,
    progress          TEXT,
    ai_current_focus  TEXT,
    ai_next_tasks     TEXT,
    ai_risks          TEXT,
    ai_advice         TEXT,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pr_user_id ON project_record(user_id);
