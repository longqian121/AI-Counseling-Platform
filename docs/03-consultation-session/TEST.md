## 模块3：咨询会话管理 · 测试文档
> **2026-07-18 修订**：senderType 值 1=用户/2=AI（Integer）；分页参数 pageNum


### TC-3.1 分页查询会话列表

| 项目 | 内容 |
|------|------|
| Method | GET /api/psychological-chat/sessions?page=1&pageSize=10 |
| 鉴权 | 需登录 |
| 预期 | 200，返回 { records: [...], total: 3, page: 1, pageSize: 10 } |
| 验证点 | records 含 sessionTitle / lastMessageContent / messageCount / lastMessageTime / userNickname |

### TC-3.2 空消息的会话

| 项目 | 内容 |
|------|------|
| 前置条件 | 创建新会话但未发消息 |
| 步骤 | GET /sessions |
| 预期 | lastMessageContent 为 null，messageCount 为 0，会话正常显示 |

### TC-3.3 获取会话消息列表

| 项目 | 内容 |
|------|------|
| Method | GET /api/psychological-chat/sessions/1/messages |
| 预期 | 返回消息数组，每条含 id / senderType / content / createdAt，按 createdAt 升序 |
| 验证点 | senderType 区分 "USER" 和 "AI" |

### TC-3.4 获取不存在的会话消息

| 项目 | 内容 |
|------|------|
| Method | GET /api/psychological-chat/sessions/99999/messages |
| 预期 | 200，返回空数组（不报错） |

### TC-3.5 删除会话

| 项目 | 内容 |
|------|------|
| Method | DELETE /api/psychological-chat/sessions/1 |
| 预期 | 200，会话及其所有消息从数据库删除 |
| 验证 | 再次 GET /sessions，被删除会话不在列表中 |

### TC-3.6 删除不存在的会话

| 项目 | 内容 |
|------|------|
| Method | DELETE /api/psychological-chat/sessions/99999 |
| 预期 | 200（幂等，不报错）或 BusinessException |

### TC-3.7 获取会话情绪分析（有数据）

| 项目 | 内容 |
|------|------|
| 前置条件 | conversation_session.last_emotion_analysis 存有有效 JSON |
| Method | GET /api/psychological-chat/session/1/emotion |
| 预期 | 返回 primaryEmotion / emotionScore / isNegative / riskLevel / suggestion / improvementSuggestions / riskDescription |

### TC-3.8 获取会话情绪分析（无数据）

| 项目 | 内容 |
|------|------|
| 前置条件 | last_emotion_analysis 为 null |
| Method | GET /api/psychological-chat/session/2/emotion |
| 预期 | 200，data 为 null 或空对象 |

### TC-3.9 越权访问

| 项目 | 内容 |
|------|------|
| 前置条件 | 用户 A 登录，用户 B 的会话 |
| 步骤 | GET /sessions/{B的会话ID}/messages |
| 预期 | 本阶段不做用户级隔离（会话属于哪个用户就显示哪个） |
| 未来优化 | 按当前登录用户 userId 过滤 |

### TC-3.10 前端 Enter 键修复验证

| 项目 | 内容 |
|------|------|
| 前置条件 | consultation.vue 的 handleKeyDown 已修复，添加了 sendMessage() 调用 |
| 步骤 | 在输入框按 Enter |
| 预期 | 消息正常发送 |
