package org.example.aispringboot.DTO.command;

import lombok.Data;

@Data
public class KnowledgeArticleCreateDTO {
    private String title;
    private String content;
    private String coverImage;
    private Long categoryId;
    private String summary;
    private String tags;
}
