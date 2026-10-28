# AI Agent 工作流 —— 技术设计文档

## 1. 架构总览

    用户发消息
      ├─ [同步] 咨询师 Agent (deepseek-v4-pro) ── 流式返回对话
      └─ [异步] 风险评估 Agent (deepseek-chat)  ── 并行危机检测

    用户预约成功
      └─ [异步] 访前简报 Agent (deepseek-reasoner) ── 生成患者背景简报

    每天 02:00
      └─ [定时] 质检洞察 Agent (deepseek-reasoner) ── 全量审计+质量评分

## 2. ChatClientConfig 新增 Bean

在现有 ChatClientConfig 中新增两个 ChatClient Bean，不使用默认 advisor、独立于主对话 ChatClient。

## 3. Prompt 常量新增

在 PromptMessage.java 中新增：RISK_ASSESSMENT_PROMPT / PRE_VISIT_BRIEFING_PROMPT / QUALITY_EMPATHY_PROMPT / QUALITY_ISSUE_CLASSIFY_PROMPT

## 4. 数据库新建表

risk_alert: session_id, user_id, risk_level(0-4), risk_type, confidence, evidence, suggested_action, is_read
pre_visit_briefing: appointment_id, patient_user_id, doctor_user_id, patient_background, key_concerns, emotion_trend, risk_history, suggested_approach, is_viewed
quality_report: report_date(唯一), total_sessions, risk_alert_count, crisis_count, empathy_score, relevance_score, safety_score, overall_score, top_issues, flagged_session_ids

## 5. 新增文件清单

RiskAssessmentService / RiskAlert / RiskAlertMapper / PreVisitBriefingService / PreVisitBriefing / PreVisitBriefingMapper / QualityInspectionService / QualityReport / QualityReportMapper

## 6. API 端点

GET /api/admin/risk-alert/page
PUT /api/admin/risk-alert/{id}/read
GET /api/doctor/briefing/{appointmentId}
GET /api/admin/quality/report/latest

## 7. 现有改动项

- PsychologicalSupportService: doOnComplete 中加 riskAssessmentService.assessAsync()
- AppointmentService: 预约成功后触发 preVisitBriefingService.generateBriefing()
- AiSpringbootApplication: 已有 @EnableScheduling