package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.aispringboot.AiService.PromptMessage;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.ConsultationSummary;
import org.example.aispringboot.mapper.ConsultationMessageMapper;
import org.example.aispringboot.mapper.ConsultationSummaryMapper;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ConsultationSummaryService {

    @Resource
    private ConsultationMessageMapper messageMapper;

    @Resource
    private ConsultationSummaryMapper summaryMapper;

    @Resource
    private consultationSessionMapper sessionMapper;

    @Autowired
    @Qualifier("openAiChatClient")
    private ChatClient chatClient;

    public ConsultationSummary generateSummary(Long sessionId) {
        List<ConsultationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ConsultationMessage>()
                        .eq(ConsultationMessage::getSessionId, sessionId)
                        .orderByAsc(ConsultationMessage::getCreatedAt));

        String dialogText = messages.stream()
                .map(m -> (m.getSenderType() == 1 ? "用户：" : "AI：") + m.getContent())
                .collect(Collectors.joining("\n\n"));

        String raw = chatClient.prompt()
                .system(PromptMessage.CONSULTATION_SUMMARY_PROMPT)
                .user(dialogText)
                .call()
                .content();

        ConsultationSummary summary = parseSummary(raw, sessionId);

        ConsultationSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            summary.setUserId(session.getUserId());
        }

        summaryMapper.insert(summary);

        if (session != null) {
            session.setStatus("COMPLETED");
            sessionMapper.updateById(session);
        }

        return summary;
    }

    public ConsultationSummary getBySessionId(Long sessionId) {
        return summaryMapper.selectOne(
                new LambdaQueryWrapper<ConsultationSummary>()
                        .eq(ConsultationSummary::getSessionId, sessionId));
    }

    public List<ConsultationSummary> getMySummaries(Long userId) {
        return summaryMapper.selectList(
                new LambdaQueryWrapper<ConsultationSummary>()
                        .eq(ConsultationSummary::getUserId, userId)
                        .orderByDesc(ConsultationSummary::getCreatedAt));
    }

    public List<ConsultationSummary> getPatientSummaries(Long patientUserId) {
        return summaryMapper.selectList(
                new LambdaQueryWrapper<ConsultationSummary>()
                        .eq(ConsultationSummary::getUserId, patientUserId)
                        .orderByDesc(ConsultationSummary::getCreatedAt));
    }

    private ConsultationSummary parseSummary(String raw, Long sessionId) {
        ConsultationSummary s = new ConsultationSummary();
        s.setSessionId(sessionId);
        s.setContent(raw);
        s.setKeyIssues(extractSection(raw, "主要问题"));
        s.setEmotionalState(extractSection(raw, "情绪分析"));
        s.setSuggestions(extractSection(raw, "AI 建议"));
        s.setFollowUpGoals(extractSection(raw, "后续目标"));
        return s;
    }

    private String extractSection(String text, String title) {
        Pattern p = Pattern.compile("【" + title + "】(.*?)(?=【|$)", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }
}
