package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.service.EmotionDiaryService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.DTO.response.EmotionDiaryPageDTO;

import java.util.Map;

@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {
    @Resource
    private EmotionDiaryService emotionDiaryService;

    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody EmotionDiaryCreateDTO dto) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        Long id = emotionDiaryService.createOrUpdate(userId, dto);
        return Result.ok(Map.of("id", id));
    }


    @GetMapping("/admin/page")
    public Result<Page<EmotionDiaryPageDTO>> adminPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String moodScreRange) {
        Page<EmotionDiaryPageDTO> page = emotionDiaryService.getAdminPage(current, size, userId, moodScreRange);
        return Result.ok(page);
    }

    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        emotionDiaryService.adminDelete(id);
        return Result.ok();
    }

}
