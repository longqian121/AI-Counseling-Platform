# 模块 5：咨询师 Agent（已有）

## 1. 职责
与用户进行温暖共情的心理咨询对话，可调用预约等工具。

## 2. 状态
已实现。代码位于：
- AiService/PsychologicalSupportService.java
- controller/PsychologicalChat.java
- AiService/AgentToolService.java

## 3. 模型
deepseek-v4-pro

## 4. Prompt
见 PromptMessage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT

## 5. 功能
- 流式 SSE 返回
- ChatMemory 多轮对话记忆（30轮滑动窗口）
- Tool Calling（AgentToolService 提供预约查询等工具）
- 情绪实时分析