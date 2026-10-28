package org.example.aispringboot.DTO.response;

import lombok.Data;

import java.util.List;

@Data
public class CategoryTreeDTO {
    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer sortOrder;
    private List<CategoryTreeDTO> children;
}
