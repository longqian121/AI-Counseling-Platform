# 模块 2：访前简报 Agent

## 1. 职责
医生预约成功后，综合患者历史数据生成背景简报，推送给医生快速了解患者。

## 2. 触发时机
用户预约医生成功后（AppointmentService.createAppointment / completeAppointment 之后异步触发）。

## 3. 模型
deepseek-reasoner（深度推理，适合多文档综合分析）

## 4. 数据输入

| 数据源 | 数量 | 用途 |
|--------|------|------|
| consultation_summary | 该患者全部 | 提取关键问题、情绪状态、AI 建议 |
| emotion_diary | 最近 10 条 | 了解情绪自我记录 |
| consultation_session.last_emotion_analysis | 最近 5 次会话 | 获取 AI 评估的情绪 JSON |
| risk_alert | 该患者全部 | 了解风险历史 |

## 5. 系统 Prompt

```
你是一位资深心理医生助理。请根据以下患者的咨询历史数据，生成一份来访者背景简报。

数据包含：
1. 历史咨询总结（主要问题、情绪分析、AI建议、后续目标）
2. 情绪日记记录
3. AI 情绪评估记录
4. 风险告警记录（如有）

请生成以下内容：
1.【患者背景】：基于咨询历史推断的患者基本画像（不要编造，写"信息不足"如果无法推断）
2.【核心关切】：归纳患者反复提及的核心心理问题（最多 5 条）
3.【情绪轨迹】：患者情绪变化趋势（改善/稳定/恶化），引用具体数据点
4.【风险提示】：如果有历史风险告警，简要说明风险等级和类型
5.【沟通建议】：基于 AI 对话经验，给医生 3 条具体的沟通策略建议

输出格式：用【患者背景】【核心关切】【情绪轨迹】【风险提示】【沟通建议】作为标题。
```

## 6. 实现方案

### PreVisitBriefingService.java
- 注入 deepseekReasonerClient, ConsultationSummaryService, EmotionDiaryMapper, RiskAlertMapper
- generateBriefing(Appointment appointment): 聚合数据 → LLM 调用 → 写入 pre_visit_briefing 表

### AppointmentService 改动
预约成功后异步触发：
```
CompletableFuture.runAsync(() -> preVisitBriefingService.generateBriefing(appointment));
```

## 7. 后端 API

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/doctor/briefing/{appointmentId} | GET | 医生查看指定预约的简报 |
| /api/doctor/briefing/my-patient/{userId} | GET | 医生查看患者最新简报（兜底接口） |

## 8. 医生端前端改动

### 8.1 doctorAppointments.vue 改动
在预约列表操作列中，每条记录新增"简报"按钮：
```
<el-button type="success" text @click="viewBriefing(scope.row)">
  查看简报
</el-button>
```
点击后弹出 el-dialog 展示简报内容（结构化5个模块渲染）。

如果该预约尚未生成简报（is_viewed 为 null 或记录不存在），显示"简报生成中..."或"暂无简报"。

### 8.2 doctorDashboard.vue 改动（今日预约页）
同样在每行预约记录中增加"简报"按钮，逻辑同上。

### 8.3 router/index.js
医生端已有路由，无需新增。简报通过弹窗/对话框展示，不新开页面。

### 8.4 doctor.js 新增 API 函数
```
export function getBriefing(appointmentId) { return service.get('/doctor/briefing/' + appointmentId) }
export function getLatestBriefing(userId) { return service.get('/doctor/briefing/my-patient/' + userId) }
```

### 8.5 简报弹窗组件结构
el-dialog title="来访者背景简报"，内部分 5 个模块渲染：
- 【患者背景】el-tag 基本信息
- 【核心关切】el-tag 列表
- 【情绪轨迹】趋势描述 + emoji 指示
- 【风险提示】红色警告区（如有）
- 【沟通建议】编号列表

关闭弹窗时调用后端标记 is_viewed = 1。

## 9. 数据流
```
用户预约成功 → AppointmentService
  └─ preVisitBriefingService.generateBriefing()
       ├─ 查 consultation_summary (userId)
       ├─ 查 emotion_diary (userId, limit 10)
       ├─ 查 consultation_session.last_emotion_analysis (userId, limit 5)
       ├─ 查 risk_alert (userId)
       ├─ 拼接上下文 → deepseek-reasoner 调用
       └─ 写入 pre_visit_briefing 表
            └─ 医生端 doctorAppointments / doctorDashboard 点击"查看简报"
                 └─ el-dialog 结构化渲染 5 个模块
```