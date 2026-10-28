# AI Agent 工作流 —— 需求文档

## 1. 项目背景

当前系统已具备基础 AI 咨询能力：用户与 AI 对话、情绪分析、咨询总结自动生成。本需求旨在将系统从"单 Agent 被动回复"升级为"多 Agent 协作工作流"，提升系统的主动性、安全性和洞察力。

## 2. 整体目标

部署 5 个职责分明的 AI Agent，通过 Spring 定时任务和事件驱动机制协作，覆盖心理健康咨询的全生命周期：对话 > 风险监测 > 总结沉淀 > 访前准备 > 质量复盘。

## 3. Agent 矩阵

| Agent | 状态 | 触发时机 | 模型 | 核心价值 |
|-------|------|----------|------|----------|
| 咨询师 Agent | 已有 | 用户发消息 | deepseek-v4-pro | 温暖共情的主对话 |
| 总结 Agent | 已有 | 会话结束 / 定时 | deepseek-chat | 结构化咨询总结 |
| 风险评估 Agent | 新增 | 每条用户消息并行 | deepseek-chat | 实时危机检测，超阈值告警 |
| 访前简报 Agent | 新增 | 用户预约成功后 | deepseek-reasoner | 为医生生成患者背景简报 |
| 质检洞察 Agent | 新增 | 每天凌晨 2:00 | deepseek-reasoner | 全量会话审计、质量评分、趋势报告 |

## 4. 各 Agent 详细需求

### 4.1 风险评估 Agent

**触发时机：** 用户每次发送消息时，与主对话 Agent 并行执行。

**检测内容：**
- 自杀意念（主动 / 被动）
- 自伤行为提及
- 严重危机信号（家庭暴力、性侵、物质滥用等）
- 绝望感、无价值感的极端表达

**输出（JSON）：**
- riskLevel（0-4）：0=正常，1=关注，2=轻度风险，3=中度风险，4=高度危机
- riskType：自杀风险 / 自伤风险 / 危机事件 / 无
- confidence（0-100）：判断置信度
- evidence：对话中识别到的关键语句
- suggestedAction：建议措施

**告警规则：**
- riskLevel >= 3 → 即时写入 risk_alert 表 + 管理员端通知
- riskLevel == 4 → 额外标记"危机"，包含紧急干预建议

### 4.2 访前简报 Agent

**触发时机：** 用户成功预约医生后立即触发。

**输入数据源：**
- 患者所有 consultation_summary 记录
- 患者最近 10 条 emotion_diary 记录
- 患者最近 5 次会话的 last_emotion_analysis

**输出：**
- patientBackground：患者基本画像
- keyConcerns：主要心理问题归纳
- emotionTrend：情绪变化趋势（改善 / 稳定 / 恶化）
- riskHistory：历史风险告警摘要
- suggestedApproach：建议医生沟通策略

**推送目标：** 写入 pre_visit_briefing 表，医生端预约管理页可查看。

### 4.3 质检洞察 Agent

**触发时机：** 每天凌晨 2:00。

**分析维度：**

| 维度 | 数据来源 | 方式 |
|------|----------|------|
| 基础统计 | SQL 聚合 | 不调用 LLM |
| 风险汇总 | risk_alert 表 | SQL 聚合 |
| 情绪趋势 | consultation_session | SQL 聚合 |
| AI 共情评分 | 随机抽样 5-10 条会话 | LLM 评分 |
| AI 安全性评分 | 同上 | LLM 评分 |
| 高频问题 TOP5 | consultation_summary.key_issues | LLM 归类 |
| 需复核案例 | 情绪恶化 / 风险会话 | LLM 标记 |

## 5. 非功能需求

- 并行性：风险评估不阻塞主对话流
- 异步性：访前简报和质检洞察为后台异步，失败不影响主流程
- 容错：任何 Agent 失败记录日志并降级
- 成本控制：deepseek-chat 用于高频简单任务，deepseek-reasoner 仅用于低频深度分析

## 6. 实施优先级

| 优先级 | Agent | 理由 |
|--------|-------|------|
| P0 | 风险评估 Agent | 安全价值最高，改动量小 |
| P1 | 访前简报 Agent | 医生端核心需求，衔接预约流程 |
| P2 | 质检洞察 Agent | 运营价值高但非紧急，独立模块 |
