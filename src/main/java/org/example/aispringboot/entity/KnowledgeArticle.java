package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_article")
public class KnowledgeArticle {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String tags;
    private Long authorId;
    private Integer readCount;
    private Integer status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 分页查询 JOIN 出的作者昵称 */
    @TableField(exist = false)
    private String authorName;
    /** 分页查询 JOIN 出的分类名 */
    @TableField(exist = false)
    private String categoryName;
}
