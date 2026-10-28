# 自动生成咨询总结 —— 技术设计文档

## 1. 数据库改动

### 1.1 新建 consultation_summary 表

```sql
CREATE TABLE consultation_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    key_issues TEXT,
    emotional_state VARCHAR(500),
    suggestions TEXT,
    follow_up_goals TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 1.2 consultation_session 表新增字段

```sql
ALTER TABLE consultation_session ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/COMPLETED';
ALTER TABLE consultation_session ADD COLUMN last_message_at DATETIME COMMENT '最后一条消息时间';
```

`last_message_at` 在每次保存消息时更新（在 `ConsultationMessageService.saveUserMessage()` 和 `saveAimessage()` 中同步更新）。

## 2. 实体

### 2.1 ConsultationSummary.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("consultation_summary")
@Builder
public class ConsultationSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("session_id") private Long sessionId;
    @TableField("user_id") private Long userId;
    private String content;
    @TableField("key_issues") private String keyIssues;
    @TableField("emotional_state") private String emotionalState;
    private String suggestions;
    @TableField("follow_up_goals") private String followUpGoals;
    @TableField("created_at") private LocalDateTime createdAt;
}
```

### 2.2 ConsultationSession.java 新增字段

```java
@TableField("status")
private String status;  // ACTIVE / COMPLETED

@TableField("last_message_at")
private LocalDateTime lastMessageAt;
```

## 3. Prompt 设计

```java
public static final String CONSULTATION_SUMMARY_PROMPT =
    "你是一位经验丰富的心理咨询督导。请根据以下对话记录，生成一份专业的咨询总结报告。\n"
    + "\n要求：\n"
    + "1.【主要问题】：列出用户提到的核心问题，每条一句话，最多 3 条。如果没有明确问题，写"用户未明确表达具体问题"。\n"
    + "2.【情绪分析】：评估用户在对话中的主要情绪状态和波动程度（稳定/中等/剧烈）。只基于对话内容判断。\n"
    + "3.【AI 建议】：提炼 AI 给出的具体建议和指导，每条一行。如果没有，写"本次对话未涉及具体建议"。\n"
    + "4.【后续目标】：列出 1-3 个建议用户在下次咨询前可以尝试的行动。\n"
    + "\n输出格式：用【主要问题】、【情绪分析】、【AI 建议】、【后续目标】作为各模块标题。";
```

## 4. 核心服务

### 4.1 ConsultationSummaryService

```java
@Service
public class ConsultationSummaryService {

    @Resource private ConsultationMessageMapper messageMapper;
    @Resource private ConsultationSummaryMapper summaryMapper;
    @Resource private consultationSessionMapper sessionMapper;
    @Resource private ChatClient chatClient;

    public ConsultationSummary generateSummary(Long sessionId) {
        // 1. 查对话记录
        List<ConsultationMessage> messages = messageMapper.selectList(
            new LambdaQueryWrapper<ConsultationMessage>()
                .eq(ConsultationMessage::getSessionId, sessionId)
                .orderByAsc(ConsultationMessage::getCreatedAt));

        String dialogText = messages.stream()
            .map(m -> (m.getSenderType() == 1 ? "用户：" : "AI：") + m.getContent())
            .collect(Collectors.joining("\n\n"));

        // 2. 调用 LLM 生成
        String raw = chatClient.prompt()
            .system(PromptMessage.CONSULTATION_SUMMARY_PROMPT)
            .user(dialogText)
            .call()
            .content();

        // 3. 解析各模块（按标题分割）
        ConsultationSummary summary = parseSummary(raw, sessionId);

        // 4. 查 userId
        ConsultationSession session = sessionMapper.selectById(sessionId);
        if (session != null) summary.setUserId(session.getUserId());

        summaryMapper.insert(summary);

        // 5. 标记会话为 COMPLETED
        if (session != null) {
            session.setStatus("COMPLETED");
            sessionMapper.updateById(session);
        }

        return summary;
    }

    private ConsultationSummary parseSummary(String raw, Long sessionId) {
        ConsultationSummary s = new ConsultationSummary();
        s.setSessionId(sessionId);
        s.setContent(raw);
        s.setKeyIssues(extractSection(raw, "主要问题"));
        s.setEmotionalState(extractSection(raw, "情绪分析"));
        s.setSuggestions(extractSection(raw, "AI 建议"));
        s.setFollowUpGoals(extractSection(raw, "后续目标"));
        return s;
    }

    private String extractSection(String text, String title) {
        // 用正则匹配【标题】到下一个【或文本结束
        Pattern p = Pattern.compile("【" + title + "】(.*?)(?=【|$)", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }
}
```

## 5. 自动结单定时任务

```java
@Component
public class ConsultationAutoCloseTask {

    @Resource private consultationSessionMapper sessionMapper;
    @Resource private ConsultationMessageMapper messageMapper;
    @Resource private ConsultationSummaryService summaryService;

    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void autoCloseInactiveSessions() {
        // 查所有 ACTIVE 的会话
        List<ConsultationSession> activeSessions = sessionMapper.selectList(
            new LambdaQueryWrapper<ConsultationSession>()
                .eq(ConsultationSession::getStatus, "ACTIVE"));

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        for (ConsultationSession session : activeSessions) {
            // 查最后一条消息时间
            ConsultationMessage lastMsg = messageMapper.selectOne(
                new LambdaQueryWrapper<ConsultationMessage>()
                    .eq(ConsultationMessage::getSessionId, session.getId())
                    .orderByDesc(ConsultationMessage::getCreatedAt)
                    .last("LIMIT 1"));

            if (lastMsg != null && lastMsg.getCreatedAt().isBefore(threshold)) {
                // 超过 10 分钟无新消息，自动结单
                summaryService.generateSummary(session.getId());
            }
        }
    }
}
```

启用定时任务需要在 `AiSpringbootApplication.java` 加 `@EnableScheduling`。

## 6. API 控制器

### 6.1 ConsultationSummaryController（用户端）

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/consultation/session/{id}/summary/generate | POST | 生成总结 |
| /api/consultation/session/{id}/summary | GET | 获取已有总结 |
| /api/consultation/my-summaries | GET | 我的总结列表 |

userId 从 JWT 提取，校验 session 归属。

### 6.2 AdminConsultationSummaryController（管理员端）

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/admin/consultation/summary/page | GET | 分页列表（支持按 userId/日期筛选） |
| /api/admin/consultation/summary/{id} | GET | 查看详情 |

### 6.3 DoctorPatientController（医生端）

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/doctor/patient/{userId}/summaries | GET | 查看指定患者的总结列表 |

医生端权限校验：通过 counselor.user_id → 查询该医生是否有此患者的预约记录，有权限才返回。

## 7. 前端改动

### 7.1 用户端 consultation.vue

| 改动 | 说明 |
|------|------|
| 底部"结束本次咨询"按钮 | 点击 → 确认 → POST generate → 展示总结弹窗 |
| 顶部导航栏新增"历史总结"图标 | 点击 → 抽屉/侧边栏列出我的所有总结，可点击展开详情 |
| 总结弹窗/组件 | 结构化渲染四个模块，支持内容换行 |

### 7.2 管理员端

| 文件 | 说明 |
|------|------|
| consultationSummaries.vue | 总结列表页（TableSearch + el-table + el-pagination） |
| router 新增 /back/summaries | 路由 + 左侧菜单 |
| admin.js 新增 API | summaryPage / getSummaryDetail |

### 7.3 医生端

| 文件 | 说明 |
|------|------|
| patientSummaries.vue | 患者总结页（路由 /doctor/patient/:userId/summaries）|
| doctorAppointments.vue | 预约列表的每行加"查看总结"按钮，点击跳转 |
| doctor.js 新增 API | getPatientSummaries(userId) |

## 8. 数据流总览

```
用户前端点击"结束咨询" OR 自动结单定时任务触发
    |
POST /api/consultation/session/{id}/summary/generate
    |
ConsultationSummaryService.generateSummary(sessionId)
    |
查 consultation_message 所有消息 → 拼接对话文本
    |
chatClient.prompt().system(SUMMARY_PROMPT).user(dialog).call()
    |
解析 LLM 返回 → 拆分为四个模块 → 写入 consultation_summary
    |
更新 consultation_session.status = COMPLETED
    |
前端展示总结弹窗
```

## 9. 实施步骤

| 步骤 | 内容 | 预估 |
|------|------|------|
| 1 | DB 迁移 + 实体更新 + Mapper | 0.25 天 |
| 2 | Prompt + ConsultationSummaryService | 0.25 天 |
| 3 | Controller（用户端 / 管理员 / 医生） | 0.25 天 |
| 4 | 自动结单 @Scheduled 定时任务 | 0.25 天 |
| 5 | 前端：用户端聊天页按钮 + 总结弹窗 + 历史入口 | 0.5 天 |
| 6 | 前端：管理员端总结列表 | 0.25 天 |
| 7 | 前端：医生端患者总结查看 | 0.25 天 |
| **合计** | | **2 天** |