## 模块4：情绪日记 · 测试文档
> **2026-07-18 修订**：确认表结构字段类型与实际一致


### TC-4.1 创建情绪日记

| 项目 | 内容 |
|------|------|
| Method | POST /api/emotion-diary |
| 鉴权 | 需登录 |
| 请求体 | { "moodScore":7, "dominantEmotion":"开心", "emotionTriggers":"完成项目", "diaryContent":"今天很有成就感", "sleepQuality":4, "stressLevel":2, "diaryDate":"2026-07-18" } |
| 预期 | 200，返回 data: { "id": 1 } |
| 验证点 | userId 自动从 token 获取，不来自请求体 |

### TC-4.2 同一天重复创建（upsert）

| 项目 | 内容 |
|------|------|
| 前置条件 | 用户当天已有日记 |
| 步骤 | 再次 POST 同日期日记 |
| 预期 | 200，日记内容更新为最新提交的值 |

### TC-4.3 评分范围校验

| 场景 | 预期 |
|------|------|
| moodScore=0 | 400，校验失败 |
| moodScore=11 | 400，校验失败 |
| sleepQuality=0 | 400，校验失败 |
| sleepQuality=6 | 400，校验失败 |
| stressLevel=6 | 400，校验失败 |

### TC-4.4 管理员分页查询

| 项目 | 内容 |
|------|------|
| Method | GET /api/emotion-diary/admin/page?page=1&pageSize=10 |
| 鉴权 | 需登录（管理员） |
| 预期 | 返回 { records, total } |
| 验证点 | records 含 id / nickname / diaryDate / moodScore / sleepQuality / stressLevel / emotionTriggers / diaryContent / aiEmotionAnalysis / createdAt / updatedAt |

### TC-4.5 管理员按昵称筛选

| 项目 | 内容 |
|------|------|
| Method | GET /api/emotion-diary/admin/page?nickname=张三 |
| 预期 | 仅返回 nickname 含"张三"的日记 |

### TC-4.6 管理员按日期筛选

| 项目 | 内容 |
|------|------|
| Method | GET /api/emotion-diary/admin/page?startDate=2026-07-01&endDate=2026-07-31 |
| 预期 | 仅返回该日期范围内的日记 |

### TC-4.7 管理员删除日记

| 项目 | 内容 |
|------|------|
| Method | DELETE /api/emotion-diary/admin/1 |
| 预期 | 200，数据库记录删除 |

### TC-4.8 普通用户尝试访问管理员接口

| 项目 | 内容 |
|------|------|
| 前置条件 | 普通用户（userType=1）登录 |
| 步骤 | GET /api/emotion-diary/admin/page |
| 预期 | 403 Forbidden 或后台校验拦截 |
