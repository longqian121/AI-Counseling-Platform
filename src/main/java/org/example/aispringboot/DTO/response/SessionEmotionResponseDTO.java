package org.example.aispringboot.DTO.response;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class SessionEmotionResponseDTO {
	private String primaryEmotion = "中性";
	private Integer emotionScore = 50;
	private Boolean isNegative = false;
	private Integer riskLevel = 0;
	private String suggestion = "情绪状态平稳";
	private List<String> improvementSuggestions = new ArrayList<>();
	private String riskDescription;
}
