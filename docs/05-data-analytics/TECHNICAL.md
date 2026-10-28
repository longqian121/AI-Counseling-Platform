## 模块5：数据统计 · 技术文档
> **2026-07-18 修订**：
> - vgDurationMinutes：当前 conversation_session 无 ended_at，改为取消息时间跨度
> - 注意 knowledge_article.id 为 UUID，无法用 id 排序


### 1. 接口清单（1 个）

| # | Method | Path | 说明 | 鉴权 |
|---|--------|------|------|------|
| 16 | GET | /api/data-analytics/overview | 仪表盘概览 | 需登录（管理员） |

### 2. 新增文件清单

| 文件 | 路径 |
|------|------|
| Controller | controller/DataAnalyticsController.java |
| Service | service/DataAnalyticsService.java |
| DTO Response | DTO/response/DashboardOverviewDTO.java |

### 3. 响应结构

`json
{
  "systemOverview": {
    "totalUsers": 150,
    "activeUsers": 45,
    "totalDiaries": 320,
    "todayNewDiaries": 8,
    "totalSessions": 500,
    "todayNewSessions": 12,
    "avgMoodScore": 6.8
  },
  "emotionTrend": [
    { "date": "2026-07-12", "avgMoodScore": 6.5, "recordCount": 15 },
    { "date": "2026-07-13", "avgMoodScore": 7.1, "recordCount": 12 }
  ],
  "consultationStats": {
    "totalSessions": 500,
    "avgDurationMinutes": 25.5,
    "dailyTrend": [
      { "date": "2026-07-12", "sessionCount": 30 },
      { "date": "2026-07-13", "sessionCount": 28 }
    ]
  },
  "userActivity": [
    { "date": "2026-07-12", "activeUsers": 10, "newUsers": 5, "diaryUsers": 8, "consultationUsers": 7 },
    { "date": "2026-07-13", "activeUsers": 12, "newUsers": 3, "diaryUsers": 9, "consultationUsers": 8 }
  ]
}
`

### 4. DTO 定义

`java
@Data
public class DashboardOverviewDTO {
    private SystemOverviewDTO systemOverview;
    private List<EmotionTrendDTO> emotionTrend;
    private ConsultationStatsDTO consultationStats;
    private List<UserActivityDTO> userActivity;
}

@Data
public static class SystemOverviewDTO {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalDiaries;
    private Long todayNewDiaries;
    private Long totalSessions;
    private Long todayNewSessions;
    private Double avgMoodScore;
}

@Data
public static class EmotionTrendDTO {
    private LocalDate date;
    private Double avgMoodScore;
    private Integer recordCount;
}

@Data
public static class ConsultationStatsDTO {
    private Long totalSessions;
    private Double avgDurationMinutes;
    private List<DailyTrendDTO> dailyTrend;
}

@Data
public static class DailyTrendDTO {
    private LocalDate date;
    private Integer sessionCount;
}

@Data
public static class UserActivityDTO {
    private LocalDate date;
    private Integer activeUsers;
    private Integer newUsers;
    private Integer diaryUsers;
    private Integer consultationUsers;
}
`

### 5. Service 层核心 SQL

#### 5.1 systemOverview

`java
// totalUsers
userMapper.selectCount(null);

// activeUsers（7 天内有会话或日记的用户）
// SELECT COUNT(DISTINCT user_id) FROM (...) UNION

// totalDiaries
emotionDiaryMapper.selectCount(null);

// todayNewDiaries
emotionDiaryMapper.selectCount(
    Wrappers.lambdaQuery(EmotionDiary.class)
        .eq(EmotionDiary::getDiaryDate, LocalDate.now()));

// totalSessions
consultationSessionMapper.selectCount(null);

// todayNewSessions
// SELECT COUNT(*) FROM consultation_session WHERE DATE(started_at) = CURDATE()

// avgMoodScore
// SELECT AVG(mood_score) FROM emotion_diary
`

#### 5.2 emotionTrend（近 7 天）

`sql
SELECT diary_date AS date, AVG(mood_score) AS avgMoodScore, COUNT(*) AS recordCount
FROM emotion_diary
WHERE diary_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY diary_date
ORDER BY diary_date ASC
`

#### 5.3 consultationStats

`sql
-- dailyTrend 近 7 天
SELECT DATE(started_at) AS date, COUNT(*) AS sessionCount
FROM consultation_session
WHERE started_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE(started_at)
ORDER BY date ASC
`

#### 5.4 userActivity（近 7 天）

需要 UNION 三个子查询：
- newUsers：user 表 created_at 按天统计
- diaryUsers：emotion_diary 表 user_id DISTINCT 按天统计
- consultationUsers：consultation_session 表 user_id DISTINCT 按天统计

### 6. 实现策略

- 所有统计通过 MyBatis Mapper 自定义 SQL 实现
- Controller 层一次调用 Service，返回完整 DashboardOverviewDTO
- 不做缓存（数据量小，实时查询即可）
- 错误处理：如果某张表为空，对应指标返回 0 或空列表

### 7. 注意事项

- 日期处理使用 LocalDate，注意时区
- vgDurationMinutes 的计算依赖会话持续时间数据，目前表中可能没有直接字段，可以用消息的时间跨度近似或返回 0
- 所有 SQL 注意索引优化（diary_date、started_at、created_at）
