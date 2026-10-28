## 模块5：数据统计 · 测试文档
> **2026-07-18 修订**：avgDurationMinutes 改为消息时间差


### TC-5.1 仪表盘概览（全量数据）

| 项目 | 内容 |
|------|------|
| Method | GET /api/data-analytics/overview |
| 鉴权 | 需登录（管理员） |
| 预期 | 200，返回完整 DashboardOverviewDTO，所有数值 >= 0 |
| 验证点 | systemOverview / emotionTrend / consultationStats / userActivity 四个节点均存在 |

### TC-5.2 仪表盘概览（空数据）

| 项目 | 内容 |
|------|------|
| 前置条件 | emotion_diary 表为空，consultation_session 表为空 |
| 预期 | totalDiaries=0，todayNewDiaries=0，avgMoodScore=null 或 0，emotionTrend=[]，dailyTrend=[] |

### TC-5.3 仪表盘近 7 天数据准确性

| 项目 | 内容 |
|------|------|
| 前置条件 | 今天插入 3 条日记（moodScore: 5,7,9） |
| 步骤 | GET /overview |
| 预期 | todayNewDiaries=3，avgMoodScore=7.0，emotionTrend 最后一条 date=今天 |

### TC-5.4 userActivity 数据准确性

| 项目 | 内容 |
|------|------|
| 前置条件 | 今天注册 2 个新用户，1 个写了日记，1 个有咨询会话 |
| 预期 | userActivity 今天：newUsers=2，diaryUsers=1，consultationUsers=1 |

### TC-5.5 普通用户访问

| 项目 | 内容 |
|------|------|
| 前置条件 | 普通用户登录 |
| 步骤 | GET /api/data-analytics/overview |
| 预期 | 403 或权限校验拦截 |

### TC-5.6 接口性能

| 项目 | 内容 |
|------|------|
| 前置条件 | 每张表 10000 条记录 |
| 步骤 | GET /overview |
| 预期 | 响应时间 < 2s |
