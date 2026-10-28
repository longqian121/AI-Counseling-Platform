package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.aispringboot.entity.KnowledgeArticle;

@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {

    @Select("<script>"
            + "SELECT a.*, u.nickname AS authorName, c.category_name AS categoryName "
            + "FROM knowledge_article a "
            + "LEFT JOIN user u ON a.author_id = u.id "
            + "LEFT JOIN knowledge_category c ON a.category_id = c.id "
            + "<where>"
            + "<if test='title != null and title != \"\"'>AND a.title LIKE CONCAT('%', #{title}, '%')</if>"
            + "<if test='categoryId != null'>AND a.category_id = #{categoryId}</if>"
            + "<if test='status != null'>AND a.status = #{status}</if>"
            + "</where>"
            + "ORDER BY a.created_at DESC"
            + "</script>")
    IPage<KnowledgeArticle> selectArticlePage(Page<?> page,
                                              @Param("title") String title,
                                              @Param("categoryId") Long categoryId,
                                              @Param("status") Integer status);
}
