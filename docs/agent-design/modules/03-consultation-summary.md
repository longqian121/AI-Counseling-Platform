# 模块 3：咨询总结 Agent（已有）

## 1. 职责
会话结束后自动生成结构化咨询总结：主要问题 / 情绪分析 / AI 建议 / 后续目标。

## 2. 状态
已实现。代码位于：
- entity/ConsultationSummary.java
- service/ConsultationSummaryService.java
- task/ConsultationAutoCloseTask.java
- controller/ConsultationSummaryController.java

## 3. 模型
deepseek-chat

## 4. Prompt
见 PromptMessage.CONSULTATION_SUMMARY_PROMPT

## 5. 触发方式
- 用户点击"结束本次咨询"→ POST /api/consultation/session/{id}/summary/generate
- 自动结单：@Scheduled(fixedDelay=60000)，10分钟无新消息自动结单并生成总结