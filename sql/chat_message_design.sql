-- ============================================================
-- AI 聊天平台 - 聊天记录表设计文档
-- 表名: chat_message
-- 说明: 存储用户与 AI 之间的所有对话消息
-- ============================================================

-- 如果表已存在，先删除（生产环境慎用！）
-- DROP TABLE IF EXISTS chat_message;

CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '消息唯一ID',
    user_id     BIGINT       NOT NULL                 COMMENT '用户ID，关联 user 表',
    role        VARCHAR(20)  NOT NULL                 COMMENT '消息角色：user=用户消息, assistant=AI回复',
    content     TEXT         NOT NULL                 COMMENT '消息内容（文本）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='聊天消息记录表';

-- ============================================================
-- 字段详解
-- ============================================================

-- id: BIGINT, 主键自增
--   每条消息的唯一标识。使用 BIGINT 而非 INT 是因为聊天消息量可能很大，
--   INT 最大值约 21 亿，BIGINT 可支持更大数据量。
--   自增主键保证插入顺序与消息顺序一致。

-- user_id: BIGINT, NOT NULL
--   关联到 user 表的 id 字段，标识这条消息属于哪个用户。
--   通过这个字段可以查询某个用户的所有聊天记录。
--   注意：这里没有设置外键约束（FOREIGN KEY），原因：
--   1. 外键会影响写入性能（每次插入都要检查 user 表）
--   2. 项目中通过业务代码保证数据一致性
--   3. 后续分库分表时外键会成为障碍

-- role: VARCHAR(20), NOT NULL
--   消息角色，只有两个值：
--   - 'user'：用户发送的消息
--   - 'assistant'：AI 回复的消息
--   用 VARCHAR(20) 而非 ENUM 是因为：
--   1. ENUM 修改枚举值需要 ALTER TABLE，很麻烦
--   2. VARCHAR 更灵活，后续如果增加角色（如 system）无需改表结构
--   3. 代码层面做校验即可

-- content: TEXT, NOT NULL
--   消息的具体内容。使用 TEXT 类型而非 VARCHAR 是因为：
--   1. 用户可能发送长文本（如粘贴代码、文章）
--   2. AI 回复可能很长（DeepSeek 支持 1M 上下文）
--   3. VARCHAR 最大 65535 字节，TEXT 最大 65535 字符
--   如果后续需要存储超长内容（>65535字符），可改为 MEDIUMTEXT 或 LONGTEXT

-- create_time: DATETIME, NOT NULL, DEFAULT CURRENT_TIMESTAMP
--   消息的创建时间。用于：
--   1. 按时间排序显示聊天记录
--   2. 查询最近 N 条消息作为上下文
--   3. 后续可以做时间范围查询

-- ============================================================
-- 索引设计说明
-- ============================================================

-- PRIMARY KEY (id): 主键索引
--   加速单条消息的查询，同时保证数据唯一性

-- INDEX idx_user_id (user_id): 普通索引
--   这是最常用的查询条件：查询某个用户的所有消息。
--   不加这个索引，每次查询都要全表扫描，性能极差。

-- INDEX idx_create_time (create_time): 普通索引
--   用于按时间排序和范围查询。
--   注意：查询用户最近 N 条消息时，MySQL 可能同时用到
--   user_id 和 create_time 两个索引，或者使用复合索引。
--   如果后续发现性能问题，可以改为复合索引：
--   INDEX idx_user_time (user_id, create_time)

-- ============================================================
-- 为什么这样设计
-- ============================================================

-- 1. 简单够用
--    聊天记录的核心就是：谁、说了什么、什么时候说的。
--    三个核心字段（user_id, role, content, create_time）覆盖了所有需求。

-- 2. 不做过度设计
--    - 没有 conversation_id（会话ID）：当前项目每次对话独立，
--      通过时间排序和历史记录拼接上下文，不需要显式分组。
--    - 没有 token_count（消耗Token数）：后续需要统计用量时可再加。
--    - 没有 parent_id（父消息ID）：当前是线性对话，不需要树形结构。

-- 3. 性能考虑
--    - user_id 加索引：最常用的查询条件
--    - create_time 加索引：排序和范围查询
--    - 使用 utf8mb4：支持中文、emoji 等所有 Unicode 字符
--    - TEXT 类型：支持长文本，但注意 TEXT 字段不能有默认值

-- 4. 扩展性
--    - 后续如果需要会话分组，加 conversation_id 字段即可
--    - 后续如果需要消息关联（引用/回复），加 parent_id 字段即可
--    - 后续如果需要存储图片/文件，加 media_url 字段即可

-- ============================================================
-- 这个表在整个项目中的作用
-- ============================================================

-- 1. 聊天记录持久化
--    用户和 AI 的对话不会丢失，刷新页面后仍可查看历史消息。

-- 2. 上下文记忆
--    AI 聊天时，从这张表取出最近 N 条消息作为上下文发送给 DeepSeek API，
--    让 AI 记住之前的对话内容，实现连贯的对话体验。

-- 3. 数据统计
--    可以统计：用户活跃度、消息数量、使用频率等。

-- 4. 后续功能基础
--    - 聊天历史页面：按时间展示所有对话
--    - 搜索功能：搜索历史消息内容
--    - 导出功能：导出聊天记录

-- ============================================================
-- 常用查询 SQL 示例
-- ============================================================

-- 1. 查询某个用户的最近 10 条消息（按时间倒序）
-- SELECT * FROM chat_message
-- WHERE user_id = ?
-- ORDER BY create_time DESC
-- LIMIT 10;

-- 2. 查询某个用户的所有消息（按时间正序）
-- SELECT * FROM chat_message
-- WHERE user_id = ?
-- ORDER BY create_time ASC;

-- 3. 统计某个用户的总消息数
-- SELECT COUNT(*) FROM chat_message WHERE user_id = ?;

-- 4. 统计所有用户的消息总数
-- SELECT COUNT(*) FROM chat_message;

-- 5. 查询某段时间内的消息
-- SELECT * FROM chat_message
-- WHERE create_time BETWEEN '2026-01-01 00:00:00' AND '2026-12-31 23:59:59';
