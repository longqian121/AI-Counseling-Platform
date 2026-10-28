package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("emotion_diary")
public class EmotionDiary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate diaryDate;
    private Integer moodScore;
    private String dominantEmotion;
    @TableField("emotion_triggers")
    private String emotionTriggers;
    @TableField("diary_content")
    private String diaryContent;
    private Integer sleepQuality;
    private Integer stressLevel;
    @TableField("ai_emotion_analysis")
    private String aiEmotionAnalysis;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
