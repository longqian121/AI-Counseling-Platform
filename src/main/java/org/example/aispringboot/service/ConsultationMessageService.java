package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.mapper.ConsultationMessageMapper;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsultationMessageService {
	@Autowired
	private ConsultationMessageMapper consultationMessageMapper;

	@Autowired
	private consultationSessionMapper consultationSessionMapper;

	public ConsultationMessage saveUserMessage(Long sessionId, String content, String emotion_tag) {
		// 保存用户消息
		ConsultationMessage userMessage = ConsultationMessage.builder()
				.sessionId(sessionId)
				.senderType(1)
				.messageType(1)
				.content(content)
				.emotionTag(emotion_tag)
				.createdAt(LocalDateTime.now())
				.build();

		consultationMessageMapper.insert(userMessage);

		updateSessionLastMessageAt(sessionId);

		return userMessage;
	}

	public ConsultationMessage saveAimessage(Long sessionId, String content, String aiModel) {

		ConsultationMessage message = ConsultationMessage.builder()
				.sessionId(sessionId)
				.senderType(2)
				.messageType(1)
				.content(content)
				.aiModel(aiModel)
				.createdAt(LocalDateTime.now())
				.build();
		// 保存AI消息
		consultationMessageMapper.insert(message);

		updateSessionLastMessageAt(sessionId);

		return message;
	}


    public void deleteBySessionId(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId);
        consultationMessageMapper.delete(queryWrapper);
    }

	public Integer getMessageCountBySessionId(Long sessionId) {
		LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ConsultationMessage::getSessionId, sessionId);

		Long count = consultationMessageMapper.selectCount(queryWrapper);
		return count.intValue();
	}

	// 获取会话最后一条消息
	public ConsultationMessageResponseDTO getLastMessageBySessionId(Long sessionId) {
		LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ConsultationMessage::getSessionId, sessionId)
				.orderByDesc(ConsultationMessage::getCreatedAt)
				.last("limit 1");

		ConsultationMessage lastMessage = consultationMessageMapper.selectOne(queryWrapper);
		return lastMessage != null ? convertToResponseDTO(lastMessage) : null;

	}

	public List<ConsultationMessageResponseDTO> getMessagesBySessionId(Long sessionId) {
		LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ConsultationMessage::getSessionId, sessionId)
				.orderByAsc(ConsultationMessage::getCreatedAt);

		List<ConsultationMessage> messages = consultationMessageMapper.selectList(queryWrapper);
		return messages.stream()
				.map(this::convertToResponseDTO)
				.collect(Collectors.toList());
	}

	private void updateSessionLastMessageAt(Long sessionId) {
		ConsultationSession session = consultationSessionMapper.selectById(sessionId);
		if (session != null) {
			session.setLastMessageAt(java.time.LocalDateTime.now());
			consultationSessionMapper.updateById(session);
		}
	}

	private ConsultationMessageResponseDTO convertToResponseDTO(ConsultationMessage message) {
		if (message == null) {
			return null;
		}

		// 转换为响应DTO
		ConsultationMessageResponseDTO responseDTO = new ConsultationMessageResponseDTO();
		responseDTO.setId(message.getId());
		responseDTO.setSessionId(message.getSessionId());
		responseDTO.setSenderType(message.getSenderType());
		responseDTO.setMessageType(message.getMessageType());
		responseDTO.setContent(message.getContent());
		responseDTO.setEmotionTag(message.getEmotionTag());
		responseDTO.setAiModel(message.getAiModel());
		responseDTO.setCreatedAt(message.getCreatedAt());

		// 设置发送者类型描述
		responseDTO.setSenderTypeDesc(message.getSenderTypeDesc());
		responseDTO.setMessageTypeDesc(message.getMessageTypeDesc());

		// 计算消息长度
		responseDTO.calculateContentLength();

		return responseDTO;
	}
}
