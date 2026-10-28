package org.example.aispringboot.controller;

import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.aispringboot.AiService.PsychologicalSupportService;
import org.example.aispringboot.AiService.StructOutPut;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.command.ConsultationStreamDTO;
import org.example.aispringboot.DTO.response.SessionListResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.example.aispringboot.service.ConsultationSessionService;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.DTO.response.SessionEmotionResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.enumClass.UserType;
import java.util.List;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChat {
	@Autowired
	private PsychologicalSupportService psychologicalSupportService;
	@Autowired
	private consultationSessionMapper consultationSessionMapper;
	@Autowired
	private ConsultationSessionService consultationSessionService;
	@Autowired
	private ConsultationMessageService consultationMessageService;

	@PostMapping("/session/start")
	public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
		//闁兼儳鍢茶ぐ鍥亹閹惧啿顤呴柣顫妽閸?
		String token = JwtTokenUtil.getCurrentToken();
		DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
		Long userId = jwt.getClaim("userId").asLong();
		StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
		return Result.ok(session);
	}

	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
		// 流式输出心理咨询结果
		String token = JwtTokenUtil.getCurrentToken();
		DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
		Long userId = jwt.getClaim("userId").asLong();

		if (userId == null) {
			return Flux.just(ServerSentEvent.<String>builder()
					.event("error")
						.data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg(), "User not logged in or expired")))
					.build());
		}

		// 流式输出心理咨询结果
return psychologicalSupportService.streamPsychologicalChat(streamDTO.getSessionId(), streamDTO.getUserMessage())
				.map(Fragment -> {
					return ServerSentEvent.<String>builder()
							.event("message")
							.data(JSONUtil.toJsonStr(Result.ok(Map.of("content", Fragment, "type", "normal"))))
							.build();
				})
				.concatWith(Flux.just(ServerSentEvent.<String>builder()
						.event("done")
						.data("{}")
						.build()
				))
				.delayElements(Duration.ofMillis(50));
	}

	@GetMapping("/sessions")
	public Result<IPage<SessionListResponseDTO>> getSessions(
			@RequestParam(required = false, defaultValue = "1") Integer pageNum,
			@RequestParam(required = false, defaultValue = "10") Integer pageSize) {
		String token = JwtTokenUtil.getCurrentToken();
		DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
		Long userId = jwt.getClaim("userId").asLong();

		Page<SessionListResponseDTO> page = new Page<>(pageNum, pageSize);
		IPage<SessionListResponseDTO> result = consultationSessionMapper.selectSessionPage(page, userId);
		return Result.ok(result);
	}

	@DeleteMapping("/sessions/{sessionId}")
	public Result<Void> deleteSession(@PathVariable Long sessionId) {
		String token = JwtTokenUtil.getCurrentToken();
		DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
		Long userId = jwt.getClaim("userId").asLong();

		consultationSessionService.deleteSession(sessionId, userId);
		return Result.ok();
	}

	@GetMapping("/sessions/{sessionId}/messages")
	public Result<List<ConsultationMessageResponseDTO>> getSessionMessages(@PathVariable Long sessionId) {
		String token = JwtTokenUtil.getCurrentToken();
		DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
		Long userId = jwt.getClaim("userId").asLong();
		String adminAuthority = "ROLE_" + UserType.ADMIN.getCode();
		boolean admin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
				.stream().anyMatch(authority -> adminAuthority.equals(authority.getAuthority()));
		if (!admin) {
			ConsultationSession session = consultationSessionMapper.selectById(sessionId);
			if (session == null || !userId.equals(session.getUserId())) {
				throw new AccessDeniedException("无权查看该会话");
			}
		}

		List<ConsultationMessageResponseDTO> messages = consultationMessageService.getMessagesBySessionId(sessionId);
		return Result.ok(messages);
	}

	@GetMapping("/session/{sessionId}/emotion")
	public Result<SessionEmotionResponseDTO> getSessionEmotion(@PathVariable String sessionId) {
		String token = JwtTokenUtil.getCurrentToken();
		DecodedJWT jwt = JwtTokenUtil.verifyToken(token);

		Long dbSessionId = psychologicalSupportService.extractSessionId(sessionId);
		if (dbSessionId == null) {
			return Result.error(ResultCode.PARAM_INVALID.getCode(), "Session ID format error", null);
		}

		SessionEmotionResponseDTO emotion = consultationSessionService.getSessionEmotion(dbSessionId);
		return Result.ok(emotion);
	}

	@GetMapping("/admin/sessions")
	public Result<IPage<SessionListResponseDTO>> getAdminSessions(
			@RequestParam(required = false, defaultValue = "1") Integer pageNum,
			@RequestParam(required = false, defaultValue = "10") Integer pageSize,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) Long sessionId) {
		Page<SessionListResponseDTO> page = new Page<>(pageNum, pageSize);
		Map<String, Object> params = new HashMap<>();
		if (userId != null) params.put("userId", userId);
		if (sessionId != null) params.put("sessionId", sessionId);
		IPage<SessionListResponseDTO> result = consultationSessionMapper.selectAdminSessionPage(page, params);
		return Result.ok(result);
	}

}
