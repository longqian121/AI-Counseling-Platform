# 自动生成咨询总结 —— 需求与技术设计文档

## 1. 功能概述

用户和心理 AI 聊完天后，AI 自动把这次聊天内容提炼成一份结构化的咨询总结报告，存到数据库里。用户可在聊天页查看、管理员可在后台查阅、心理医生可在预约管理页查看自己患者的总结以便快速了解来访者背景。

## 2. 用户故事

| 角色 | 故事 |
|------|------|
| 普通用户 | 和心理 AI 聊完后点"结束咨询"，看到一份结构化的总结，知道自己聊了什么、AI 给了什么建议 |
| 普通用户 | 聊天页有个小入口随时可以翻看之前的对话总结 |
| 管理员 | 在后台查看所有用户的咨询报告总结，了解整体咨询质量 |
| 心理医生 | 在预约管理页看到即将来访的患者，点击查看其历史咨询总结，快速了解患者情况 |

## 3. 触发时机（两个触发源）

### 3.1 用户主动结束（方案 A）

用户在前端聊天页点击"结束本次咨询"按钮 → 立即为该会话生成总结。

- 按钮位置：咨询聊天页底部
- 交互流程：点击 → 确认弹窗 → 生成中 loading → 弹窗展示总结内容
- 生成后：`consultation_session` 标记为 COMPLETED，`consultation_summary` 写入数据库

### 3.2 自动结单（方案 C）

会话超过 **10 分钟** 无新消息 → 系统自动结单 + 生成总结。

- 实现方式：Spring @Scheduled 定时任务，每分钟扫描一次
- 判断逻辑：`consultation_session` 中最后一条消息的 `created_at` 距离当前时间超过 10 分钟
- 自动结单后同样生成总结 + 标记会话为 COMPLETED
- 用户下次进入聊天页时看到总结弹窗

## 4. 新增实体

### 4.1 ConsultationSummary（咨询总结）

```java
@TableName("consultation_summary")
public class ConsultationSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;        // 关联的咨询会话
    private Long userId;           // 用户ID（冗余，方便查询）
    private String content;        // 完整总结文本
    private String keyIssues;      // 主要问题（JSON 数组字符串）
    private String emotionalState; // 情绪状态摘要
    private String suggestions;    // AI 建议
    private String followUpGoals;  // 后续目标
    private LocalDateTime createdAt;
}
```

## 5. 总结内容结构

AI 生成的总结按以下四个模块组织：

| 模块 | 说明 |
|------|------|
| 主要问题 | 用户提到的核心问题，每条一句话，最多 3 条 |
| 情绪分析 | 用户在对话中表现出的主要情绪状态和波动程度 |
| AI 建议 | AI 在对话中给出的具体建议和指导 |
| 后续目标 | 建议用户在下次咨询前可以尝试的行动 |

## 6. API 端点

| 端点 | 方法 | 角色 | 说明 |
|------|------|------|------|
| /api/consultation/session/{id}/summary/generate | POST | 用户 | 为指定会话生成总结 |
| /api/consultation/session/{id}/summary | GET | 用户 | 查看已有总结 |
| /api/consultation/my-summaries | GET | 用户 | 查看我的所有总结列表 |
| /api/admin/consultation/summary/page | GET | 管理员 | 分页查看所有总结 |
| /api/admin/consultation/summary/{id} | GET | 管理员 | 查看总结详情 |
| /api/doctor/patient/{userId}/summaries | GET | 医生 | 查看指定患者的总结列表 |

## 7. 数据库建表

```sql
CREATE TABLE consultation_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    key_issues TEXT COMMENT 'JSON 数组，主要问题',
    emotional_state VARCHAR(500) COMMENT '情绪状态',
    suggestions TEXT COMMENT 'AI 建议',
    follow_up_goals TEXT COMMENT '后续目标',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 8. 前端改动

### 8.1 用户端

| 改动 | 说明 |
|------|------|
| 聊天页底部"结束本次咨询"按钮 | 点击生成总结 + 弹窗展示 |
| 聊天页顶部小图标入口"历史总结" | 点击列出该用户所有总结，可展开查看 |
| 总结展示组件 | 结构化渲染四个模块，支持 HTML 格式 |

### 8.2 管理员端

| 改动 | 说明 |
|------|------|
| 左侧菜单新增"咨询总结" | 路由 /back/summaries |
| 列表页 | 表格展示（用户昵称、会话标题、生成时间），点击查看详情弹窗 |

### 8.3 医生端

| 改动 | 说明 |
|------|------|
| 预约列表每行增加"查看总结"按钮 | 点击跳转到该患者的总结列表 |
| 患者总结页 | 展示该患者的所有历史总结，按时间降序 |

## 9. 实施步骤

| 步骤 | 内容 | 预估 |
|------|------|------|
| 1 | DB 建表 + ConsultationSummary 实体 + Mapper | 0.25 天 |
| 2 | ConsultationSummaryService + Prompt + 生成逻辑 | 0.5 天 |
| 3 | 自动结单定时任务 | 0.25 天 |
| 4 | 用户端 API + 结束咨询按钮 + 总结展示 | 0.5 天 |
| 5 | 管理员端总结列表 | 0.25 天 |
| 6 | 医生端患者总结查看 | 0.25 天 |
| **合计** | | **2 天** |