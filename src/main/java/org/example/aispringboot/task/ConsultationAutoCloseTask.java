package org.example.aispringboot.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.mapper.ConsultationMessageMapper;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.example.aispringboot.service.ConsultationSummaryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ConsultationAutoCloseTask {

    @Resource
    private consultationSessionMapper sessionMapper;

    @Resource
    private ConsultationMessageMapper messageMapper;

    @Resource
    private ConsultationSummaryService summaryService;

    @Scheduled(fixedDelay = 60000)
    public void autoCloseInactiveSessions() {
        List<ConsultationSession> activeSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>()
                        .eq(ConsultationSession::getStatus, "ACTIVE"));

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        for (ConsultationSession session : activeSessions) {
            ConsultationMessage lastMsg = messageMapper.selectOne(
                    new LambdaQueryWrapper<ConsultationMessage>()
                            .eq(ConsultationMessage::getSessionId, session.getId())
                            .orderByDesc(ConsultationMessage::getCreatedAt)
                            .last("LIMIT 1"));

            if (lastMsg != null && lastMsg.getCreatedAt().isBefore(threshold)) {
                try {
                    summaryService.generateSummary(session.getId());
                } catch (Exception e) {
                    System.err.println("Auto close session " + session.getId() + " failed: " + e.getMessage());
                }
            }
        }
    }
}
