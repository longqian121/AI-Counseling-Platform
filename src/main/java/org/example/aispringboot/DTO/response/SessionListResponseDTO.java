package org.example.aispringboot.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionListResponseDTO {
    private Long id;
    private Long userId;
    private String sessionTitle;
    private LocalDateTime startedAt;
    private String lastMessageContent;
    private Integer messageCount;
    private LocalDateTime lastMessageTime;
    private String userNickname;
}
