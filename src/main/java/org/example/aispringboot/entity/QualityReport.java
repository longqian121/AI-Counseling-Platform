package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("quality_report")
@Builder
public class QualityReport {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_date")
    private LocalDate reportDate;

    @TableField("total_sessions")
    private Integer totalSessions;

    @TableField("risk_alert_count")
    private Integer riskAlertCount;

    @TableField("crisis_count")
    private Integer crisisCount;

    @TableField("empathy_score")
    private BigDecimal empathyScore;

    @TableField("relevance_score")
    private BigDecimal relevanceScore;

    @TableField("safety_score")
    private BigDecimal safetyScore;

    @TableField("overall_score")
    private BigDecimal overallScore;

    @TableField("top_issues")
    private String topIssues;

    @TableField("flagged_session_ids")
    private String flaggedSessionIds;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
