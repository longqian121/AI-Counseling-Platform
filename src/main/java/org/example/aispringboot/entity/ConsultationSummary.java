package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("consultation_summary")
@Builder
public class ConsultationSummary {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("user_id")
    private Long userId;

    private String content;

    @TableField("key_issues")
    private String keyIssues;

    @TableField("emotional_state")
    private String emotionalState;

    private String suggestions;

    @TableField("follow_up_goals")
    private String followUpGoals;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
