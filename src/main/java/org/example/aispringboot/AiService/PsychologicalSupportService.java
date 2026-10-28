package org.example.aispringboot.AiService;

import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Map;
import java.time.LocalDate;
import java.time.DayOfWeek;
import cn.hutool.json.JSONUtil;
import org.example.aispringboot.DTO.response.SessionEmotionResponseDTO;
import java.util.List;


@Service
public class PsychologicalSupportService {
	@Autowired
	private ChatMemory chatMemory;

	@Autowired
	@Qualifier("openAiChatClient")
	private ChatClient chatClient;

	@Autowired
	private AgentToolService agentToolService;

	@Autowired
	private consultationSessionMapper consultationSessionMapper;

	@Autowired
	private ConsultationSessionService consultationSessionService;

	@Autowired
	private ConsultationMessageService consultationMessageService;

	@Autowired
	private RiskAssessmentService riskAssessmentService;

	public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO){
		ConsultationSession consultationSession = consultationSessionService.createSession(userId, createDTO);

		consultationMessageService.saveUserMessage(consultationSession.getId(), createDTO.getInitialMessage(), null);

		String sessionId = "session_" + consultationSession.getId();
		return new StructOutPut.StreamChatSession(
				sessionId,
				userId,
				createDTO.getInitialMessage(),
				System.currentTimeMillis(),
				System.currentTimeMillis() + 86400000L,
				1,
				"ACTIVE"
		);
	}

	public Flux<String> streamPsychologicalChat(String sessionId, String userMessage){
		return Flux.create(sink -> {
			Long dbSessionId = extractSessionId(sessionId);
			if(dbSessionId == null){
				sink.error(new RuntimeException("session ID format error"));
				return;
			}

			boolean isInitialMessage = false;
			Integer messageCount = consultationMessageService.getMessageCountBySessionId(dbSessionId);
			if(messageCount == 1){
				ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
				if(lastMessage != null && userMessage.equals(lastMessage.getContent()) && lastMessage.getSenderType() == 1){
					isInitialMessage = true;
				}
			}
			if(!isInitialMessage){
				consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
			}

			ConsultationSession currentSession = consultationSessionMapper.selectById(dbSessionId);
			Map<String, Object> toolContext = Map.of("userId",
					(Object) (currentSession != null && currentSession.getUserId() != null
							? currentSession.getUserId() : 0L));
			ToolCallingChatOptions toolOptions = ToolCallingChatOptions.builder()
					.toolContext(toolContext)
					.build();

			String conversationId = "conversation-" + sessionId;
			List<Message> userMessages = new ArrayList<>();
			userMessages.add(new UserMessage(userMessage));
			chatMemory.add(conversationId,userMessages);
			Prompt prompt = new Prompt(List.of(
					new SystemMessage(PromptMessage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT + getDateContext())
			));

			StringBuilder fullResponse = new StringBuilder();

			chatClient.prompt(prompt)
					.user(userMessage)
					.tools(agentToolService)
					.options(toolOptions)
					.advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,conversationId))
					.stream()
					.content()
					.doOnNext(Fragment -> {
						fullResponse.append(Fragment);
						sink.next(Fragment);
					})
					.doOnComplete(() -> {
						String completeRes = fullResponse.toString();
						consultationMessageService.saveAimessage(dbSessionId, completeRes, "openai");
						List<Message> aiMessages = new ArrayList<>();
						aiMessages.add(new AssistantMessage(completeRes));
						chatMemory.add(conversationId,aiMessages);

						try {
							analyzeAndSaveEmotion(dbSessionId, userMessage, completeRes);
							Long userId = currentSession != null ? currentSession.getUserId() : null;
							riskAssessmentService.assessAsync(dbSessionId, userId, userMessage);
						} catch (Exception e) {
							System.out.println("emotion analysis failed: " + e.getMessage());
						}

						sink.complete();
					})
					.doOnError(error -> {
						sink.error(error);
					})
					.subscribe();
		});
	}

	private static String getDateContext() {
		LocalDate today = LocalDate.now();
		LocalDate maxDate = today.plusDays(14);
		String dayOfWeek = switch (today.getDayOfWeek()) {
			case MONDAY -> "星期一";
			case TUESDAY -> "星期二";
			case WEDNESDAY -> "星期三";
			case THURSDAY -> "星期四";
			case FRIDAY -> "星期五";
			case SATURDAY -> "星期六";
			case SUNDAY -> "星期日";
		};
		return "\n\n【系统信息】当前日期：" + today + " " + dayOfWeek
			+ "。可预约日期范围：" + today + " 至 " + maxDate + "。"
			+ "请注意：用户在提到日期时请基于此当前日期计算，不要凭记忆猜测。"
+ "例如用户说「周五」，请用工具查询系统日期对应的是几号，而不是自己推测。";
	}

	public Long extractSessionId(String sessionId){
		if(sessionId != null && sessionId.startsWith("session_")){
			String idStr = sessionId.substring("session_".length());
			return Long.parseLong(idStr);
		}
		return null;
	}


	private void analyzeAndSaveEmotion(Long dbSessionId, String userMessage, String aiResponse) {
		try {
			String conversation = "user: " + userMessage + "\nAI: " + aiResponse;
			String rawResult = chatClient.prompt()
					.system(PromptMessage.EMOTION_ANALYSIS_PROMPT)
					.user(conversation)
					.call()
					.content();

			String json = extractJson(rawResult);
			JSONUtil.parseObj(json);
			consultationSessionService.updateEmotionAnalysis(dbSessionId, json);
		} catch (Exception e) {
			String defaultJson = JSONUtil.toJsonStr(new SessionEmotionResponseDTO());
			consultationSessionService.updateEmotionAnalysis(dbSessionId, defaultJson);
		}
	}

	private String extractJson(String text) {
		if (text == null) return "";
		int start = text.indexOf("{");
		int end = text.lastIndexOf("}");
		if (start >= 0 && end > start) {
			return text.substring(start, end + 1).trim();
		}
		return text.trim();
	}

}
