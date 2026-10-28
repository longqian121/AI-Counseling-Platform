## 模块5：数据统计 · 变更记录

> 2026-07-18

### 新增文件

| 文件 | 路径 |
|------|------|
| DTO | `src/main/java/org/example/aispringboot/DTO/response/DashboardOverviewDTO.java` |
| Service | `src/main/java/org/example/aispringboot/service/DataAnalyticsService.java` |
| Controller | `src/main/java/org/example/aispringboot/controller/DataAnalyticsController.java` |

### 修改文件

| 文件 | 变更内容 |
|------|----------|
| `mapper/EmotionDiaryMapper.java` | 新增 `selectEmotionTrend(days)` 方法 — 近 N 天按天统计平均情绪分和记录数 |
| `mapper/consultationSessionMapper.java` | 新增 `selectDailyTrend(days)` 方法 — 近 N 天按天统计会话数 |
| `mapper/ConsultationMessageMapper.java` | 新增 `selectSessionDurations()` 方法 — 按会话统计首末消息时间差 |
| `mapper/UserMapper.java` | 新增 `selectNewUsersByPeriod(days)` 方法 — 近 N 天按天统计新增用户数 |

### 接口

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/data-analytics/overview` | 仪表盘概览，需管理员权限（roleType=2） |

### 实现要点

- **鉴权**：从 JWT `roleType` claim 判断管理员身份（`roleType == 2`），非管理员返回 `A0301`
- **activeUsers**：取近 7 天日记活跃用户数与咨询活跃用户数的较大值作为估算
- **avgDurationMinutes**：遍历所有 consultation_message 记录，按 session_id 分组计算首/末消息时间差，取平均值；无消息数据的会话不参与计算
- **userActivity**：newUsers 从 user.create_at 统计，diaryUsers/consultationUsers 从各自表按天 DISTINCT 用户统计
- **emotionTrend/consultationStats.dailyTrend**：统一查询近 7 天数据
- **空数据处理**：对应表为空时，avgMoodScore 返回 null，emotionTrend/dailyTrend 返回空列表

### 注意事项

- EmotionDiaryMapper 返回的 `diary_date` 字段为 SQL DATE 类型，MyBatis 默认映射为 `java.sql.Date`，Service 层通过 `toLocalDate()` 统一转换为 `LocalDate`
- 编译通过，54 个源文件无错误
