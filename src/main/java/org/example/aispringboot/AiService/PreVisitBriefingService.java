package org.example.aispringboot.AiService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.entity.*;
import org.example.aispringboot.mapper.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class PreVisitBriefingService {

    @Resource
    @Qualifier("deepseekReasonerClient")
    private ChatClient deepseekReasonerClient;

    @Resource
    private ConsultationSummaryMapper summaryMapper;

    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    @Resource
    private RiskAlertMapper riskAlertMapper;

    @Resource
    private PreVisitBriefingMapper briefingMapper;

    @Resource
    private AppointmentMapper appointmentMapper;

    @Resource
    private consultationSessionMapper sessionMapper;

    public void generateBriefingAsync(Appointment appointment) {
        CompletableFuture.runAsync(() -> {
            try {
                Long count = briefingMapper.selectCount(
                        new LambdaQueryWrapper<PreVisitBriefing>()
                                .eq(PreVisitBriefing::getAppointmentId, appointment.getId()));
                if (count != null && count > 0) {
                    System.out.println("[Briefing] Already exists for appointment " + appointment.getId());
                    return;
                }
                System.out.println("[Briefing] Starting for appointment " + appointment.getId() + ", user " + appointment.getUserId());
                generateAndSave(appointment);
                System.out.println("[Briefing] Completed for appointment " + appointment.getId());
            } catch (Exception e) {
                System.out.println("[Briefing] FAILED for appointment " + appointment.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void generateAndSave(Appointment appointment) {
        Long userId = appointment.getUserId();

        List<ConsultationSummary> summaries = summaryMapper.selectList(
                new LambdaQueryWrapper<ConsultationSummary>()
                        .eq(ConsultationSummary::getUserId, userId)
                        .orderByDesc(ConsultationSummary::getCreatedAt));

        List<EmotionDiary> diaries = emotionDiaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>()
                        .eq(EmotionDiary::getUserId, userId)
                        .orderByDesc(EmotionDiary::getCreatedAt)
                        .last("LIMIT 10"));

        List<ConsultationSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>()
                        .eq(ConsultationSession::getUserId, userId)
                        .isNotNull(ConsultationSession::getLastEmotionAnalysis)
                        .orderByDesc(ConsultationSession::getStartedAt)
                        .last("LIMIT 5"));

        List<RiskAlert> alerts = riskAlertMapper.selectList(
                new LambdaQueryWrapper<RiskAlert>()
                        .eq(RiskAlert::getUserId, userId)
                        .orderByDesc(RiskAlert::getCreatedAt));

        StringBuilder context = new StringBuilder();
        context.append("=== 历史咨询总结 ===\n");
        for (ConsultationSummary s : summaries) {
            context.append("主要问题：").append(s.getKeyIssues() != null ? s.getKeyIssues() : "").append("\n");
            context.append("情绪状态：").append(s.getEmotionalState() != null ? s.getEmotionalState() : "").append("\n");
            context.append("AI建议：").append(s.getSuggestions() != null ? s.getSuggestions() : "").append("\n");
            context.append("后续目标：").append(s.getFollowUpGoals() != null ? s.getFollowUpGoals() : "").append("\n\n");
        }

        context.append("=== 情绪日记记录 ===\n");
        for (EmotionDiary d : diaries) {
            context.append("日期：").append(d.getDiaryDate()).append("\n");
            context.append("情绪评分：").append(d.getMoodScore()).append("\n");
            context.append("主要情绪：").append(d.getDominantEmotion() != null ? d.getDominantEmotion() : "").append("\n");
            context.append("日记内容：").append(d.getDiaryContent() != null ? d.getDiaryContent() : "").append("\n\n");
        }

        context.append("=== AI 情绪评估记录 ===\n");
        for (ConsultationSession s : sessions) {
            context.append("评估：").append(s.getLastEmotionAnalysis() != null ? s.getLastEmotionAnalysis() : "").append("\n");
        }

        context.append("=== 风险告警记录 ===\n");
        for (RiskAlert a : alerts) {
            context.append("风险等级：").append(a.getRiskLevel()).append("\n");
            context.append("风险类型：").append(a.getRiskType() != null ? a.getRiskType() : "").append("\n");
            context.append("证据：").append(a.getEvidence() != null ? a.getEvidence() : "").append("\n\n");
        }

        System.out.println("[Briefing] Calling deepseek-reasoner for user " + userId);
        String raw = deepseekReasonerClient.prompt()
                .system(PromptMessage.PRE_VISIT_BRIEFING_PROMPT)
                .user(context.toString())
                .call()
                .content();
        System.out.println("[Briefing] deepseek-reasoner responded, length=" + (raw != null ? raw.length() : 0));

        PreVisitBriefing briefing = PreVisitBriefing.builder()
                .appointmentId(appointment.getId())
                .patientUserId(appointment.getUserId())
                .doctorUserId(appointment.getCounselorId())
                .patientBackground(extractSection(raw, "患者背景"))
                .keyConcerns(extractSection(raw, "核心关切"))
                .emotionTrend(extractSection(raw, "情绪轨迹"))
                .riskHistory(extractSection(raw, "风险提示"))
                .suggestedApproach(extractSection(raw, "沟通建议"))
                .isViewed(0)
                .createdAt(LocalDateTime.now())
                .build();

        briefingMapper.insert(briefing);
        System.out.println("[Briefing] Saved to DB for appointment " + appointment.getId());
    }

    private String extractSection(String text, String title) {
        if (text == null) return "";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "【" + title + "】(.*?)(?=【|$)", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }
}
