## 模块4：情绪日记 · 技术文档
> **2026-07-18 修订**：
> - 表已存在，含 UNIQUE(user_id, diary_date)
> - mood_score TINYINT NOT NULL；sleep_quality/stress_level TINYINT
> - emotion_triggers TEXT 非 VARCHAR(1000)
> - 外键 ON DELETE CASCADE
> - 额外字段 i_analysis_updated_at DATETIME


### 1. 接口清单（3 个）

| # | Method | Path | 说明 | 鉴权 |
|---|--------|------|------|------|
| 13 | POST | /api/emotion-diary | 创建情绪日记 | 需登录（用户） |
| 14 | GET | /api/emotion-diary/admin/page | 管理员分页查询 | 需登录（管理员） |
| 15 | DELETE | /api/emotion-diary/admin/{id} | 管理员删除 | 需登录（管理员） |

### 2. SQL DDL

`sql
CREATE TABLE emotion_diary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    diary_date DATE NOT NULL,
    mood_score INT COMMENT '情绪评分 1-10',
    dominant_emotion VARCHAR(50) COMMENT '主要情绪',
    emotion_triggers VARCHAR(1000) COMMENT '情绪触发因素',
    diary_content TEXT COMMENT '日记内容',
    sleep_quality INT COMMENT '睡眠质量 1-5',
    stress_level INT COMMENT '压力水平 1-5',
    ai_emotion_analysis TEXT COMMENT 'AI 情绪分析结果 JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_diary_date (diary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
`

### 3. 新增文件清单

| 文件 | 路径 |
|------|------|
| Entity | entity/EmotionDiary.java |
| Mapper | mapper/EmotionDiaryMapper.java |
| Service | service/EmotionDiaryService.java |
| Controller | controller/EmotionDiaryController.java |
| DTO Command | DTO/command/EmotionDiaryCreateDTO.java |
| DTO Response | DTO/response/EmotionDiaryPageDTO.java |

### 4. 核心实现

#### 4.1 EmotionDiary 实体

`java
@Data
@TableName("emotion_diary")
public class EmotionDiary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate diaryDate;
    private Integer moodScore;
    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;
    private Integer sleepQuality;
    private Integer stressLevel;
    private String aiEmotionAnalysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
`

#### 4.2 POST /emotion-diary — 创建日记

1. 从 token 获取 userId
2. DTO 校验：moodScore 1-10，sleepQuality 1-5，stressLevel 1-5
3. 如 diaryDate 未传，默认当天
4. 同一用户同一天仅允许一条记录（userId + diaryDate 唯一），如果已存在则更新
5. 插入 emotion_diary 表

#### 4.3 GET /emotion-diary/admin/page — 管理员分页

1. 分页查询，支持筛选参数：nickname（JOIN user 表 LIKE）、startDate / endDate
2. MyBatis-Plus 分页 + 自定义 SQL JOIN

`java
@Select("<script>" +
    "SELECT ed.*, u.nickname " +
    "FROM emotion_diary ed LEFT JOIN user u ON ed.user_id = u.id " +
    "WHERE 1=1 " +
    "<if test='nickname != null and nickname != \"\"'>AND u.nickname LIKE CONCAT('%', #{nickname}, '%')</if>" +
    "<if test='startDate != null'>AND ed.diary_date >= #{startDate}</if>" +
    "<if test='endDate != null'>AND ed.diary_date <= #{endDate}</if>" +
    "ORDER BY ed.diary_date DESC" +
    "</script>")
IPage<EmotionDiaryPageDTO> selectAdminPage(Page<?> page,
    @Param("nickname") String nickname,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate);
`

#### 4.4 DELETE /emotion-diary/admin/{id} — 管理员删除

`java
emotionDiaryMapper.deleteById(id);
`

### 5. 注意事项

- 用户端创建日记使用当前登录用户 ID，不从前端接收 userId
- 管理员接口建议加 @PreAuthorize("hasRole('ADMIN')") 或手动校验 userType
- 同一用户同一天日记做 upsert 处理（先查后插/更）
- i_emotion_analysis 字段预留，当前阶段传 null，后续可由 AI 异步填充

---
### 数据库验证 (2026-07-18): emotion_diary already exists with UNIQUE(user_id, diary_date), mood_score TINYINT, sleep_quality/stress_level TINYINT, emotion_triggers TEXT, FK user CASCADE
