### Goal
在现有 Spring Boot 3.2 + Vue 3 AI 聊天平台上，增量开发4个"个人成长"模块：
1. Daily Review（每日复盘）
2. Learning Tracker（学习追踪）
3. Expression Training（表达训练）
4. Project Assistant（项目推进）

所有新增模块**不修改任何现有文件的核心逻辑**，仅追加路由和导航。

### Assumptions
- 现有 `user` 表已存在，复用 `user_id` 关联
- 现有 JWT 认证拦截器已经拦截 `/api/**`，新增接口自动受保护
- 现有 GlobalExceptionHandler 能统一处理新 Controller 的异常
- Element Plus 已安装，前端可用其组件
- DeepSeek API 调用方式沿用 `DeepSeekService` 的模式

### Plan

#### Step 1: 创建 4 张数据库表 + 初始化 SQL
- Files: `backend/sql/growth_os.sql` (新建)
- Change: 创建 `daily_review`、`learning_record`、`expression_record`、`project_record` 四张表，含 user_id 外键和索引
- Verify: 在 MySQL 中执行 `source backend/sql/growth_os.sql` 无报错，`SHOW TABLES` 能看到4张表

#### Step 2: 创建 4 个 Entity 类
- Files: `backend/src/main/java/com/al/aichat/entity/DailyReview.java`, `LearningRecord.java`, `ExpressionRecord.java`, `ProjectRecord.java` (新建)
- Change: 各 entity 用 `@Data` + `@TableName`，字段映射数据库列，`@TableField(fill = FieldFill.INSERT)` 用于 createdAt
- Verify: `mvn compile -q` 通过

#### Step 3: 创建 4 个 Mapper 接口
- Files: `backend/src/main/java/com/al/aichat/mapper/DailyReviewMapper.java`, `LearningRecordMapper.java`, `ExpressionRecordMapper.java`, `ProjectRecordMapper.java` (新建)
- Change: 继承 `BaseMapper<Entity>`，空接口即可
- Verify: `mvn compile -q` 通过

#### Step 4: 创建模块1 — DailyReviewService + DailyReviewController
- Files: `backend/src/main/java/com/al/aichat/service/DailyReviewService.java`, `controller/DailyReviewController.java` (新建)
- Change: 
  - `POST /api/daily-review/submit` — 用户提交复盘内容，调用 DeepSeek 生成总结
  - `GET /api/daily-review/list` — 获取历史复盘列表
  - `GET /api/daily-review/{id}` — 获取单条复盘详情
  - Service 中构造 prompt 发给 DeepSeek（格式：四段式复盘 → 四段式输出）
- Verify: `mvn compile -q` 通过

#### Step 5: 创建模块2 — LearningService + LearningController
- Files: `backend/src/main/java/com/al/aichat/service/LearningService.java`, `controller/LearningController.java` (新建)
- Change:
  - `POST /api/learning/checkin` — 打卡（英语/编程/项目/锻炼，每天每种最多一次）
  - `GET /api/learning/stats` — 本周统计 + 连续打卡天数
  - `GET /api/learning/calendar?year=2026&month=5` — 月度打卡日历
- Verify: `mvn compile -q` 通过

#### Step 6: 创建模块3 — ExpressionService + ExpressionController
- Files: `backend/src/main/java/com/al/aichat/service/ExpressionService.java`, `controller/ExpressionController.java` (新建)
- Change:
  - `GET /api/expression/today` — 获取/生成今日表达主题（每天缓存一个主题）
  - `POST /api/expression/submit` — 提交表达内容，AI 分析逻辑/结构/重复/建议
  - `GET /api/expression/history` — 历史表达记录
- Verify: `mvn compile -q` 通过

#### Step 7: 创建模块4 — ProjectService + ProjectController
- Files: `backend/src/main/java/com/al/aichat/service/ProjectService.java`, `controller/ProjectController.java` (新建)
- Change:
  - `POST /api/project/submit` — 提交项目状态，AI 拆分任务/建议/优先级/风险
  - `GET /api/project/list` — 项目记录列表
  - `GET /api/project/{id}` — 单条详情
- Verify: `mvn compile -q` 通过

#### Step 8: 前端 API 层扩展
- Files: `frontend/src/api/index.js` (修改)
- Change: 在现有 API 方法下方追加 4 个模块的 API 方法（约 80 行），不修改已有方法
- Verify: `npm run build` 无报错

#### Step 9: 前端路由 + 导航扩展
- Files: `frontend/src/router/index.js` (修改), `frontend/src/App.vue` (修改)
- Change: 
  - 添加 4 条路由 `/daily-review`, `/learning`, `/expression`, `/project`
  - 添加到 authRoutes 列表
  - App.vue 无导航栏当前状态，后续 Step 单独加
- Verify: 访问 `/#/daily-review` 不报 404（页面空白没关系）

#### Step 10: 创建 4 个 Vue 页面（MVP 版本）
- Files: `frontend/src/views/DailyReview.vue`, `Learning.vue`, `Expression.vue`, `Project.vue` (新建)
- Change: 每个页面包含表单提交 + AI 结果展示 + 历史列表，使用 Element Plus 组件，风格统一沿用现有 Chat.vue 的简洁风
- Verify: 运行 `npm run dev`，各页面能正常渲染、提交数据、展示结果

#### Step 11: 前后端联调验证 + 全量测试
- Files: 无新文件
- Change: 启动后端 + 前端，逐模块测试完整流程
- Verify: `mvn test` 全量通过，手动点击4个页面均能正常操作

### Risks & mitigations
- **DeepSeek API 费用**：每个模块都有 AI 调用，需确认限流拦截器覆盖新接口 → Step 8 顺便把新接口加到限流路径
- **数据库迁移**：新表不依赖旧表结构变更，无风险
- **前端打包**：新增路由使用懒加载 `() => import(...)`，不影响首屏

### Rollback plan
- 删除新增的 4 个 Entity/Mapper/Service/Controller 文件
- 删除新增的 4 个 Vue 页面文件
- 回滚 `api/index.js`、`router/index.js`、`App.vue` 的修改
- 删除 4 张新表 `DROP TABLE daily_review, learning_record, expression_record, project_record`
