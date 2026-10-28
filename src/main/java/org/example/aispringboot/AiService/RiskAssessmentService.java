package org.example.aispringboot.AiService;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.example.aispringboot.entity.RiskAlert;
import org.example.aispringboot.mapper.RiskAlertMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class RiskAssessmentService {

    @Resource
    @Qualifier("deepseekChatClient")
    private ChatClient deepseekChatClient;

    @Resource
    private RiskAlertMapper riskAlertMapper;

    public void assessAsync(Long sessionId, Long userId, String userMessage) {
        CompletableFuture.runAsync(() -> {
            try {
                String raw = deepseekChatClient.prompt()
                        .system(PromptMessage.RISK_ASSESSMENT_PROMPT)
                        .user(userMessage)
                        .call()
                        .content();

                String json = extractJson(raw);
                JSONObject obj = JSONUtil.parseObj(json);

                int riskLevel = obj.getInt("riskLevel", 0);
                if (riskLevel >= 3) {
                    RiskAlert alert = RiskAlert.builder()
                            .sessionId(sessionId)
                            .userId(userId)
                            .riskLevel(riskLevel)
                            .riskType(obj.getStr("riskType", ""))
                            .confidence(obj.getInt("confidence", 0))
                            .evidence(obj.getStr("evidence", ""))
                            .suggestedAction(obj.getStr("suggestedAction", ""))
                            .isRead(0)
                            .createdAt(LocalDateTime.now())
                            .build();
                    riskAlertMapper.insert(alert);
                }
            } catch (Exception e) {
                System.err.println("Risk assessment failed: " + e.getMessage());
            }
        });
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
}
