package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.exception.BusinessException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.DTO.response.EmotionDiaryPageDTO;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.example.aispringboot.mapper.EmotionDiaryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class EmotionDiaryService {
    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    public Long createOrUpdate(Long userId, EmotionDiaryCreateDTO dto) {
        validateCreateDTO(dto);

        LocalDate diaryDate = dto.getDiaryDate() != null ? dto.getDiaryDate() : LocalDate.now();

        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
               .eq(EmotionDiary::getDiaryDate, diaryDate);
        EmotionDiary existing = emotionDiaryMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setMoodScore(dto.getMoodScore());
            existing.setDominantEmotion(dto.getDominantEmotion());
            existing.setEmotionTriggers(dto.getEmotionTriggers());
            existing.setDiaryContent(dto.getDiaryContent());
            existing.setSleepQuality(dto.getSleepQuality());
            existing.setStressLevel(dto.getStressLevel());
            emotionDiaryMapper.updateById(existing);
            return existing.getId();
        }

        EmotionDiary diary = new EmotionDiary();
        diary.setUserId(userId);
        diary.setDiaryDate(diaryDate);
        diary.setMoodScore(dto.getMoodScore());
        diary.setDominantEmotion(dto.getDominantEmotion());
        diary.setEmotionTriggers(dto.getEmotionTriggers());
        diary.setDiaryContent(dto.getDiaryContent());
        diary.setSleepQuality(dto.getSleepQuality());
        diary.setStressLevel(dto.getStressLevel());
        emotionDiaryMapper.insert(diary);
        return diary.getId();
    }

    private void validateCreateDTO(EmotionDiaryCreateDTO dto) {
        Integer moodScore = dto.getMoodScore();
        if (moodScore == null || moodScore < 1 || moodScore > 10) {
            throw new BusinessException("情绪评分�?-10");
        }

        Integer sleepQuality = dto.getSleepQuality();
        if (sleepQuality != null && (sleepQuality < 1 || sleepQuality > 5)) {
            throw new BusinessException("睡眠质量�?-5");
        }

        Integer stressLevel = dto.getStressLevel();
        if (stressLevel != null && (stressLevel < 1 || stressLevel > 5)) {
            throw new BusinessException("压力水平�?-5");
        }
    }


    public Page<EmotionDiaryPageDTO> getAdminPage(int current, int size, Long userId, String moodScreRange) {
        Page<EmotionDiaryPageDTO> page = new Page<>(current, size);
        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            params.put("userId", userId);
        }
        if (moodScreRange != null && moodScreRange.contains("-")) {
            String[] range = moodScreRange.split("-");
            params.put("moodScoreMin", Integer.parseInt(range[0]));
            params.put("moodScoreMax", Integer.parseInt(range[1]));
        }
        List<EmotionDiaryPageDTO> list = emotionDiaryMapper.selectAdminPage(params);
        page.setRecords(list);
        page.setTotal(list.size());
        return page;
    }

    public void adminDelete(Long id) {
        emotionDiaryMapper.deleteById(id);
    }

}