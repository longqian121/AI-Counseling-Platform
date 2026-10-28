package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.example.aispringboot.DTO.command.KnowledgeArticleCreateDTO;
import org.example.aispringboot.DTO.command.KnowledgeArticleUpdateStatusDTO;
import org.example.aispringboot.DTO.response.CategoryTreeDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.KnowledgeArticle;
import org.example.aispringboot.service.KnowledgeArticleService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeArticleController {

    @Resource
    private KnowledgeArticleService knowledgeArticleService;

    @GetMapping("/category/tree")
    public Result<List<CategoryTreeDTO>> getCategoryTree() {
        List<CategoryTreeDTO> tree = knowledgeArticleService.getCategoryTree();
        return Result.ok(tree);
    }

    @GetMapping("/article/page")
    public Result<IPage<KnowledgeArticle>> getArticlePage(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        IPage<KnowledgeArticle> page = knowledgeArticleService.getArticlePage(currentPage, size, title, categoryId, status);
        return Result.ok(page);
    }

    @GetMapping("/article/{id}")
    public Result<KnowledgeArticle> getArticle(@PathVariable String id) {
        KnowledgeArticle article = knowledgeArticleService.getArticleById(id);
        return Result.ok(article);
    }

    @PostMapping("/article")
    public Result<KnowledgeArticle> createArticle(@RequestBody KnowledgeArticleCreateDTO dto) {
        Long userId = getCurrentUserId();
        KnowledgeArticle article = knowledgeArticleService.createArticle(dto, userId);
        return Result.ok(article);
    }

    @PutMapping("/article/{id}")
    public Result<KnowledgeArticle> updateArticle(@PathVariable String id,
                                                   @RequestBody KnowledgeArticleCreateDTO dto) {
        KnowledgeArticle article = knowledgeArticleService.updateArticle(id, dto);
        return Result.ok(article);
    }

    @PutMapping("/article/{id}/status")
    public Result<Void> updateArticleStatus(@PathVariable String id,
                                             @RequestBody KnowledgeArticleUpdateStatusDTO dto) {
        knowledgeArticleService.updateArticleStatus(id, dto.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@PathVariable String id) {
        knowledgeArticleService.deleteArticle(id);
        return Result.ok();
    }

    private Long getCurrentUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }
}
