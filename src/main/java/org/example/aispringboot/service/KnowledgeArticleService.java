package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispringboot.DTO.command.KnowledgeArticleCreateDTO;
import org.example.aispringboot.DTO.response.CategoryTreeDTO;
import org.example.aispringboot.entity.KnowledgeArticle;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.KnowledgeArticleMapper;
import org.example.aispringboot.mapper.KnowledgeCategoryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeArticleService {

    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;

    public List<CategoryTreeDTO> getCategoryTree() {
        List<KnowledgeCategory> all = knowledgeCategoryMapper.selectList(null);
        List<KnowledgeCategory> sorted = all.stream()
                .sorted((a, b) -> {
                    int sa = a.getSortOrder() != null ? a.getSortOrder() : 0;
                    int sb = b.getSortOrder() != null ? b.getSortOrder() : 0;
                    return Integer.compare(sa, sb);
                })
                .collect(Collectors.toList());
        for (KnowledgeCategory node : sorted) {
            for (KnowledgeCategory child : sorted) {
                if (node.getId().equals(child.getParentId())) {
                    node.getChildren().add(child);
                }
            }
        }
        return sorted.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0L)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private CategoryTreeDTO toDTO(KnowledgeCategory entity) {
        CategoryTreeDTO dto = new CategoryTreeDTO();
        dto.setId(entity.getId());
        dto.setCategoryName(entity.getCategoryName());
        dto.setParentId(entity.getParentId());
        dto.setSortOrder(entity.getSortOrder());
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            dto.setChildren(entity.getChildren().stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public IPage<KnowledgeArticle> getArticlePage(int currentPage, int size,
                                                   String title, Long categoryId, Integer status) {
        Page<KnowledgeArticle> page = new Page<>(currentPage, size);
        return knowledgeArticleMapper.selectArticlePage(page, title, categoryId, status);
    }

    public KnowledgeArticle getArticleById(String id) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        return article;
    }

    public KnowledgeArticle createArticle(KnowledgeArticleCreateDTO dto, Long userId) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setCategoryId(dto.getCategoryId());
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCoverImage(dto.getCoverImage());
        article.setTags(dto.getTags());
        article.setAuthorId(userId);
        article.setReadCount(0);
        article.setStatus(1);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        knowledgeArticleMapper.insert(article);
        return article;
    }

    public KnowledgeArticle updateArticle(String id, KnowledgeArticleCreateDTO dto) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setCategoryId(dto.getCategoryId());
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCoverImage(dto.getCoverImage());
        article.setTags(dto.getTags());
        article.setUpdatedAt(LocalDateTime.now());
        knowledgeArticleMapper.updateById(article);
        return article;
    }

    public void updateArticleStatus(String id, Integer status) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setStatus(status);
        article.setUpdatedAt(LocalDateTime.now());
        knowledgeArticleMapper.updateById(article);
    }

    public void deleteArticle(String id) {
        knowledgeArticleMapper.deleteById(id);
    }
}
