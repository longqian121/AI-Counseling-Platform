package org.example.aispringboot.DTO.command;

import lombok.Data;

@Data
public class KnowledgeArticleUpdateStatusDTO {
    /** 0-草稿 1-发布 */
    private Integer status;
}
