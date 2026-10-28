## 模块3：咨询会话管理 · 技术文档
> **2026-07-18 修订**：
> - 分页参数名：前端传 pageNum，后端收 pageNum（非 page）
> - 情绪分析接口：前端 path variable 带 session_ 前缀，需 Controller 内部 strip
> - senderType 为 Integer(1/2)，非字符串；复用已有 DTO
> - consultation_message 有外键 ON DELETE CASCADE，删会话自动删消息
> - last_emotion_analysis 字段为 MySQL JSON 类型


### 1. 接口清单（4 个）

| # | Method | Path | 说明 | 鉴权 |
|---|--------|------|------|------|
| 9 | GET | /api/psychological-chat/sessions | 分页查询会话列表 | 需登录 |
| 10 | GET | /api/psychological-chat/sessions/{id}/messages | 会话消息列表 | 需登录 |
| 11 | DELETE | /api/psychological-chat/sessions/{id} | 删除会话 | 需登录 |
| 12 | GET | /api/psychological-chat/session/{id}/emotion | 会话情绪分析 | 需登录 |

### 2. 新增文件清单

| 文件 | 路径 |
|------|------|
| DTO Response | DTO/response/SessionListResponseDTO.java |
| DTO Response | DTO/response/SessionEmotionDTO.java |
| 修改 Controller | controller/PsychologicalChat.java（追加 4 个方法） |

> 复用现有 ConsultationSession、ConsultationMessage entity 和 mapper；
> 复用现有的 ConsultationMessageResponseDTO。

### 3. 核心实现

#### 3.1 GET /sessions — 会话分页列表

在 ConsultationSessionMapper 新增自定义分页查询：

`java
@Select("SELECT cs.id, cs.session_title, cs.started_at, " +
    "u.nickname AS userNickname, " +
    "(SELECT content FROM consultation_message WHERE session_id = cs.id ORDER BY created_at DESC LIMIT 1) AS lastMessageContent, " +
    "(SELECT created_at FROM consultation_message WHERE session_id = cs.id ORDER BY created_at DESC LIMIT 1) AS lastMessageTime, " +
    "(SELECT COUNT(*) FROM consultation_message WHERE session_id = cs.id) AS messageCount " +
    "FROM consultation_session cs " +
    "LEFT JOIN user u ON cs.user_id = u.id " +
    "ORDER BY cs.started_at DESC")
IPage<SessionListResponseDTO> selectSessionPage(Page<?> page);
`

#### 3.2 GET /sessions/{id}/messages — 消息列表

`java
List<ConsultationMessage> messages = consultationMessageMapper.selectList(
    Wrappers.<ConsultationMessage>lambdaQuery()
        .eq(ConsultationMessage::getSessionId, id)
        .orderByAsc(ConsultationMessage::getCreatedAt)
);
// 转为现有 DTO 返回
`

#### 3.3 DELETE /sessions/{id} — 删除会话

先删消息再删会话（物理删除）：
`java
consultationMessageMapper.delete(
    Wrappers.<ConsultationMessage>lambdaQuery().eq(ConsultationMessage::getSessionId, id));
consultationSessionMapper.deleteById(id);
`

#### 3.4 GET /session/{id}/emotion — 情绪分析

读取 consultation_session.last_emotion_analysis JSON 字段解析返回：
`java
SessionEmotionDTO dto = JSONUtil.toBean(session.getLastEmotionAnalysis(), SessionEmotionDTO.class);
`

### 4. DTO 定义

#### SessionListResponseDTO
- id Long
- sessionTitle String
- lastMessageContent String
- messageCount Integer
- lastMessageTime LocalDateTime
- userNickname String

#### SessionEmotionDTO
- primaryEmotion String
- emotionScore Integer
- isNegative Boolean
- iskLevel String
- suggestion String
- improvementSuggestions List\<String\>
- iskDescription String

### 5. 注意事项

- 会话删除采用物理删除，消息一并删除
- 情绪分析接口复用 last_emotion_analysis 字段
- 分页返回 MyBatis-Plus IPage 格式 { records, total, page, pageSize }
- 所有接口从已实现的 JwtTokenUtil.getCurrentToken() 获取当前用户
