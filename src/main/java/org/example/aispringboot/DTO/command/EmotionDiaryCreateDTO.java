package org.example.aispringboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmotionDiaryCreateDTO {
    @NotNull(message = "情绪评分不能为空")
    @Min(value = 1, message = "情绪评分为1-10")
    @Max(value = 10, message = "情绪评分为1-10")
    private Integer moodScore;

    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;

    @Min(value = 1, message = "睡眠质量为1-5")
    @Max(value = 5, message = "睡眠质量为1-5")
    private Integer sleepQuality;

    @Min(value = 1, message = "压力水平为1-5")
    @Max(value = 5, message = "压力水平为1-5")
    private Integer stressLevel;

    private LocalDate diaryDate;
}

