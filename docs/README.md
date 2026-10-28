## 项目模块化开发方案

**项目**：ai-springboot（Spring Boot 3.5.16 + Vue 3 + Vite）
**技术栈**：Spring Boot 3 + MyBatis-Plus + Spring Security + JWT + Spring AI + DeepSeek + MySQL
**文档日期**：2026-07-18

### 开发顺序

| 顺序 | 模块 | 接口数 | 新增表 | 预估工时 |
|------|------|--------|--------|----------|
| 0 | Bug修复 | — | — | 0.5天 |
| 1 | 知识文章 | 7 | 2 | 1.5天 |
| 2 | 文件上传 | 1 | 0 | 0.5天 |
| 3 | 咨询会话管理 | 4 | 0 | 1天 |
| 4 | 情绪日记 | 3 | 1 | 1天 |
| 5 | 数据统计 | 1 | 0 | 0.5天 |
| 6 | 用户登出 | 1 | 0 | 0.25天 |

### 模块依赖关系

```
0-Bug修复（基础设施）
  ├── 1-知识文章 ← 依赖 2-文件上传（文章封面图）
  ├── 2-文件上传（无依赖）
  ├── 3-咨询会话管理（无依赖，复用已有 entity/mapper）
  ├── 4-情绪日记 ← 依赖 3（emotion analysis 复用）
  ├── 5-数据统计 ← 依赖 1+3+4（聚合查询）
  └── 6-用户登出（无依赖）
```

### 每个模块交付物

| 文件 | 内容 |
|------|------|
| `TECHNICAL.md` | 接口定义、数据模型、SQL DDL、核心逻辑、安全配置、注意事项 |
| `TEST.md` | 测试用例清单、测试数据、预期结果、边界条件 |

### 后端项目包结构约定

```
src/main/java/org/example/aispringboot/
├── entity/          # MyBatis-Plus 实体
├── mapper/          # MyBatis-Plus Mapper 接口
├── service/         # 业务服务
├── controller/      # REST 控制器
├── DTO/command/     # 请求 DTO
├── DTO/response/    # 响应 DTO
├── config/          # Spring 配置
├── common/          # 全局异常、Result、ResultCode
├── util/            # 工具类
├── exception/       # 自定义异常
└── enumClass/       # 枚举类
```

### 现有基础设施可复用

- `Result<T>` / `ResultCode` — 统一响应格式
- `BusinessException` — 业务异常
- `GlobarExceptionHandler` — 全局异常处理
- `JwtTokenUtil.getCurrentToken()` / `JwtTokenUtil.verifyToken()` — 获取当前登录用户
- `MyBatis-Plus` — `BaseMapper<T>` + 分页插件 `Page<T>`
- 前端 `request.js` 拦截器已剥离 `Result.data`

### 数据库验证结果 (2026-07-18)

所有 3 张「缺失表」**均已存在**，无需执行 DDL：

| 表名 | 状态 | 关键差异 |
|------|------|----------|
| knowledge_category | 已存在 | 额外字段：category_code, description, status |
| knowledge_article | 已存在 | id 为 VARCHAR(36) UUID；author_id FK→user；status 仅 1 |
| emotion_diary | 已存在 | mood_score TINYINT；UNIQUE(user_id, diary_date)；FK CASCADE |

**额外发现**：
- sys_file_info 表已存在，用于文件上传记录
- consultation_session.last_emotion_analysis 为 MySQL JSON 类型
- consultation_message 有 FK ON DELETE CASCADE

### 文档修订清单 (2026-07-18 Review)

| 模块 | 修订项 |
|------|--------|
| 0-Bug | 0.1 sessionId Bug 已不存在；新增 ConsultationSession 缺 @NoArgsConstructor；Token header 不一致注意项 |
| 1-知识文章 | id: UUID 字符串；author_id 替代 author_name；status 逻辑简化 |
| 2-文件上传 | 返回 filePath 非 fileUrl；使用 sys_file_info 表 |
| 3-咨询会话 | 分页参数 pageNum；情绪分析需 strip session_ 前缀；senderType Integer |
| 4-情绪日记 | 表已存在；字段类型修正 |
| 5-数据统计 | avgDurationMinutes 改用消息时间差 |
