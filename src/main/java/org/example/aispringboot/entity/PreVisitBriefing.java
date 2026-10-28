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
@TableName("pre_visit_briefing")
@Builder
public class PreVisitBriefing {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("patient_user_id")
    private Long patientUserId;

    @TableField("doctor_user_id")
    private Long doctorUserId;

    @TableField("patient_background")
    private String patientBackground;

    @TableField("key_concerns")
    private String keyConcerns;

    @TableField("emotion_trend")
    private String emotionTrend;

    @TableField("risk_history")
    private String riskHistory;

    @TableField("suggested_approach")
    private String suggestedApproach;

    @TableField("is_viewed")
    private Integer isViewed;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
