# 模块 1：风险评估 Agent

## 1. 职责
实时检测用户消息中的自杀、自伤、危机信号，风险超阈值立即告警。

## 2. 触发时机
用户在 PsychologicalChat 中每次发送消息时，与主对话 Agent 并行执行（不阻塞流式返回）。

## 3. 模型
deepseek-chat（快速、便宜，1元/百万 token 输入）

## 4. 系统 Prompt

```
你是一位专业的心理健康危机评估师。请根据用户的最新消息，评估其风险等级。

评估维度：
1. 自杀意念：是否有主动或被动自杀想法
2. 自伤行为：是否提及自伤行为或计划
3. 危机事件：家庭暴力、性侵、物质滥用、重大丧失等
4. 绝望感：是否表达强烈绝望、无价值感、无意义感

风险等级定义：
- 0 = 正常：无任何风险信号
- 1 = 关注：有轻微负面情绪但不构成风险
- 2 = 轻度风险：有模糊的风险信号但无明确意图
- 3 = 中度风险：明确提到自我伤害想法或严重危机事件
- 4 = 高度危机：有明确的自杀计划、自伤计划或正在经历严重危机

请严格按以下 JSON 返回，不要包含其他内容：
{"riskLevel":0,"riskType":"无","confidence":100,"evidence":"","suggestedAction":""}
```

## 5. 实现方案

### RiskAssessmentService.java
- 注入 deepseekChatClient
- assessAsync(sessionId, userMessage)：用 Mono.fromCallable + Schedulers.boundedElastic() 异步执行
- 调用 LLM → 解析 JSON → riskLevel >= 3 → 写 risk_alert 表

### PsychologicalSupportService 改动
在 streamPsychologicalChat 的 doOnComplete 回调中新增：
```
riskAssessmentService.assessAsync(dbSessionId, userMessage);
```

## 6. Prompt 常量
```
public static final String RISK_ASSESSMENT_PROMPT =
    "你是一位专业的心理健康危机评估师。请根据用户的最新消息，评估其风险等级。..."
```

## 7. 后端 API

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/admin/risk-alert/page | GET | 分页查询告警列表（支持按 riskLevel/isRead 筛选） |
| /api/admin/risk-alert/{id}/read | PUT | 标记告警已读 |
| /api/admin/risk-alert/unread-count | GET | 获取未读告警数量（用于红点 badge） |

## 8. 管理员端前端改动

### 8.1 router/index.js 新增路由
```
{
  path: 'risk-alerts',
  component: () => import('@/views/riskAlerts.vue'),
  meta: { title: '风险告警', icon: 'WarningFilled' }
}
```

Sidebar 组件从 routes[0].children 自动生成菜单，加路由即出现菜单项。

### 8.2 riskAlerts.vue（新建）
页面结构：
- PageHead title="风险告警"
- el-table 展示：用户ID、会话ID、风险等级（el-tag 颜色区分）、风险类型、触发消息（截断+tooltip）、置信度、时间、已读状态
- 筛选栏：风险等级下拉、已读/未读切换
- 点击行展开详情弹窗：完整消息内容、关键证据、建议措施
- 操作栏：标记已读按钮
- 顶部轮询 /api/admin/risk-alert/unread-count，显示未读数量 badge

### 8.3 admin.js 新增 API 函数
```
export function riskAlertPage(params) { return service.get('/admin/risk-alert/page', { params }) }
export function markRiskAlertRead(id) { return service.put('/admin/risk-alert/' + id + '/read') }
export function riskAlertUnreadCount() { return service.get('/admin/risk-alert/unread-count') }
```

### 8.4 Sidebar 增强（可选）
Sidebar 组件在菜单项渲染时，对"风险告警"菜单项额外显示未读数量 badge。实现方式：在 Sidebar.vue 的 el-menu-item 循环中加判断，调用 unread-count API。

## 9. 数据流
```
用户发消息 → streamPsychologicalChat
  ├─ 主对话 (v4-pro) → 流式 SSE 返回
  └─ riskAssessmentService.assessAsync (deepseek-chat)
       └─ riskLevel >= 3 → 写 risk_alert 表
            └─ 管理员端轮询 /api/admin/risk-alert/unread-count → 红点提示
                 └─ 点击进入 /back/risk-alerts → 分页列表 + 详情弹窗
```