package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("consultation_session")
@Builder
public class ConsultationSession {
	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@Size(max = 200, message = "Session title must not exceed 200 characters")
	@TableField("session_title")
	private String sessionTitle;

	@TableField("started_at")
	private LocalDateTime startedAt;

	@TableField("last_emotion_analysis")
	private String lastEmotionAnalysis;

	@TableField("last_emotion_updated_at")
	private LocalDateTime lastEmotionUpdatedAt;

	@TableField("status")
	private String status;

	@TableField("last_message_at")
	private LocalDateTime lastMessageAt;
}
