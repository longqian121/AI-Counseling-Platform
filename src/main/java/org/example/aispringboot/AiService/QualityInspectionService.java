package org.example.aispringboot.AiService;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.entity.*;
import org.example.aispringboot.mapper.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QualityInspectionService {

    @Resource
    @Qualifier("deepseekReasonerClient")
    private ChatClient deepseekReasonerClient;

    @Resource
    private consultationSessionMapper sessionMapper;

    @Resource
    private ConsultationMessageMapper messageMapper;

    @Resource
    private ConsultationSummaryMapper summaryMapper;

    @Resource
    private RiskAlertMapper riskAlertMapper;

    @Resource
    private QualityReportMapper reportMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void executeDailyInspection() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime yesterdayStart = yesterday.atStartOfDay();
            LocalDateTime yesterdayEnd = yesterday.plusDays(1).atStartOfDay();

            QualityReport report = new QualityReport();
            report.setReportDate(yesterday);
            report.setCreatedAt(LocalDateTime.now());

            // total_sessions: 昨日有消息的会话
            List<ConsultationSession> yesterdaySessions = sessionMapper.selectList(
                    new LambdaQueryWrapper<ConsultationSession>()
                            .isNotNull(ConsultationSession::getLastMessageAt)
                            .ge(ConsultationSession::getStartedAt, yesterdayStart)
                            .lt(ConsultationSession::getStartedAt, yesterdayEnd));
            report.setTotalSessions(yesterdaySessions.size());

            // risk_alert_count
            Long alertCount = riskAlertMapper.selectCount(
                    new LambdaQueryWrapper<RiskAlert>()
                            .ge(RiskAlert::getCreatedAt, yesterdayStart)
                            .lt(RiskAlert::getCreatedAt, yesterdayEnd));
            report.setRiskAlertCount(alertCount.intValue());

            // crisis_count
            Long crisisCount = riskAlertMapper.selectCount(
                    new LambdaQueryWrapper<RiskAlert>()
                            .ge(RiskAlert::getRiskLevel, 3)
                            .ge(RiskAlert::getCreatedAt, yesterdayStart)
                            .lt(RiskAlert::getCreatedAt, yesterdayEnd));
            report.setCrisisCount(crisisCount.intValue());

            // 阶段二：LLM 质量评分
            List<ConsultationSession> completedSessions = sessionMapper.selectList(
                    new LambdaQueryWrapper<ConsultationSession>()
                            .eq(ConsultationSession::getStatus, "COMPLETED")
                            .ge(ConsultationSession::getStartedAt, yesterdayStart)
                            .lt(ConsultationSession::getStartedAt, yesterdayEnd));

            int sampleSize = Math.min(10, Math.max(5, completedSessions.size()));
            if (sampleSize > 0) {
                List<ConsultationSession> sample = sampleRandom(completedSessions, sampleSize);
                double empathySum = 0, relevanceSum = 0, safetySum = 0;
                int scoredCount = 0;
                for (ConsultationSession session : sample) {
                    try {
                        List<ConsultationMessage> messages = messageMapper.selectList(
                                new LambdaQueryWrapper<ConsultationMessage>()
                                        .eq(ConsultationMessage::getSessionId, session.getId())
                                        .orderByAsc(ConsultationMessage::getCreatedAt));

                        String dialogText = messages.stream()
                                .map(m -> (m.getSenderType() == 1 ? "用户：" : "AI：") + m.getContent())
                                .collect(Collectors.joining("\n\n"));

                        String raw = deepseekReasonerClient.prompt()
                                .system(PromptMessage.QUALITY_EMPATHY_PROMPT)
                                .user(dialogText)
                                .call()
                                .content();

                        JSONObject obj = JSONUtil.parseObj(extractJson(raw));
                        empathySum += obj.getDouble("empathy", 0.0);
                        relevanceSum += obj.getDouble("relevance", 0.0);
                        safetySum += obj.getDouble("safety", 0.0);
                        scoredCount++;
                    } catch (Exception ignored) {
                    }
                }
                if (scoredCount > 0) {
                    report.setEmpathyScore(BigDecimal.valueOf(empathySum / scoredCount)
                            .setScale(2, RoundingMode.HALF_UP));
                    report.setRelevanceScore(BigDecimal.valueOf(relevanceSum / scoredCount)
                            .setScale(2, RoundingMode.HALF_UP));
                    report.setSafetyScore(BigDecimal.valueOf(safetySum / scoredCount)
                            .setScale(2, RoundingMode.HALF_UP));
                    report.setOverallScore(BigDecimal.valueOf(
                            (empathySum + relevanceSum + safetySum) / (3.0 * scoredCount))
                            .setScale(2, RoundingMode.HALF_UP));
                }
            }

            // 阶段三：高频问题归类
            try {
                List<ConsultationSummary> yesterdaySummaries = summaryMapper.selectList(
                        new LambdaQueryWrapper<ConsultationSummary>()
                                .isNotNull(ConsultationSummary::getKeyIssues)
                                .ge(ConsultationSummary::getCreatedAt, yesterdayStart)
                                .lt(ConsultationSummary::getCreatedAt, yesterdayEnd));
                if (!yesterdaySummaries.isEmpty()) {
                    String issuesText = yesterdaySummaries.stream()
                            .map(s -> s.getKeyIssues() != null ? s.getKeyIssues() : "")
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.joining("\n"));
                    if (!issuesText.isEmpty()) {
                        String raw = deepseekReasonerClient.prompt()
                                .system(PromptMessage.QUALITY_ISSUE_CLASSIFY_PROMPT)
                                .user(issuesText)
                                .call()
                                .content();
                        JSONObject obj = JSONUtil.parseObj(extractJson(raw));
                        report.setTopIssues(obj.getJSONArray("topIssues") != null
                                ? obj.getJSONArray("topIssues").toString() : "");
                    }
                }
            } catch (Exception ignored) {
            }

            // 阶段四：需复核案例标记
            try {
                List<ConsultationSession> flagged = sessionMapper.selectList(
                        new LambdaQueryWrapper<ConsultationSession>()
                                .ge(ConsultationSession::getStartedAt, yesterdayStart)
                                .lt(ConsultationSession::getStartedAt, yesterdayEnd));
                List<Long> flaggedIds = new ArrayList<>();
                for (ConsultationSession cs : flagged) {
                    if (cs.getLastEmotionAnalysis() != null && cs.getLastEmotionAnalysis().contains("riskLevel")) {
                        try {
                            JSONObject emo = JSONUtil.parseObj(cs.getLastEmotionAnalysis());
                            if (emo.getInt("riskLevel", 0) >= 2) {
                                flaggedIds.add(cs.getId());
                                continue;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    Long rCount = riskAlertMapper.selectCount(
                            new LambdaQueryWrapper<RiskAlert>()
                                    .eq(RiskAlert::getSessionId, cs.getId())
                                    .ge(RiskAlert::getRiskLevel, 2));
                    if (rCount > 0) {
                        flaggedIds.add(cs.getId());
                    }
                }
                if (!flaggedIds.isEmpty()) {
                    report.setFlaggedSessionIds(flaggedIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")));
                }
            } catch (Exception ignored) {
            }

            reportMapper.insert(report);
            System.out.println("Quality inspection completed for " + yesterday);
        } catch (Exception e) {
            System.err.println("Quality inspection failed: " + e.getMessage());
        }
    }

    private String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        return "{}";
    }

    private <T> List<T> sampleRandom(List<T> list, int n) {
        if (list.size() <= n) {
            return new ArrayList<>(list);
        }
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.subList(0, n);
    }
}
