package org.example.aispringboot.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.entity.ConsultationSession;
import cn.hutool.json.JSONUtil;
import org.example.aispringboot.DTO.response.SessionEmotionResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.UserMapper;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConsultationSessionService {

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private consultationSessionMapper consultationSessionMapper;

	@Autowired
	private ConsultationMessageService consultationMessageService;

	public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {

		// 校验用户是否存在
		User user = userMapper.selectById(userId);
		if(user != null){
			// 用户会话
			ConsultationSession session = ConsultationSession.builder()
					.userId(userId)
					.sessionTitle(createDTO.getSessionTitle())
					.startedAt(LocalDateTime.now())
					.build();
			// 会话标题默认值
			if(StrUtil.isBlank(createDTO.getSessionTitle())){
				session.setSessionTitle(String.format("心理AI助手" + DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss.SSS")));
			}

			// 鎻掑叆鏁版嵁
			consultationSessionMapper.insert(session);
			return session;
		}
		throw new BusinessException(ResultCode.USER_NOT_EXIST.getMsg());
	}

	public void deleteSession(Long sessionId, Long userId) {
		ConsultationSession session = consultationSessionMapper.selectById(sessionId);
		if (session == null) {
			throw new BusinessException("Session not found");
		}
		if (!session.getUserId().equals(userId)) {
			throw new BusinessException("No permission to delete other user sessions");
		}
		consultationMessageService.deleteBySessionId(sessionId);
		consultationSessionMapper.deleteById(sessionId);
	}

	public SessionEmotionResponseDTO getSessionEmotion(Long sessionId) {
		ConsultationSession session = consultationSessionMapper.selectById(sessionId);
		SessionEmotionResponseDTO dto = new SessionEmotionResponseDTO();

		if (session == null) {
			return dto;
		}

		String analysis = session.getLastEmotionAnalysis();
		if (analysis != null && !analysis.isEmpty()) {
			try {
				SessionEmotionResponseDTO parsed = JSONUtil.toBean(analysis, SessionEmotionResponseDTO.class);
				if (parsed != null) {
					return parsed;
				}
			} catch (Exception e) {
				// fallback to default
			}
		}

		return dto;
	}


	public void updateEmotionAnalysis(Long sessionId, String analysisJson) {
		ConsultationSession session = consultationSessionMapper.selectById(sessionId);
		if (session != null) {
			session.setLastEmotionAnalysis(analysisJson);
			session.setLastEmotionUpdatedAt(LocalDateTime.now());
			consultationSessionMapper.updateById(session);
		}
	}

}