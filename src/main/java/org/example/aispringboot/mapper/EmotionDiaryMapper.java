package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.DTO.response.EmotionDiaryPageDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface EmotionDiaryMapper extends BaseMapper<EmotionDiary> {

    @Select("SELECT diary_date, AVG(mood_score) AS avgMoodScore, COUNT(*) AS recordCount FROM emotion_diary WHERE diary_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY diary_date ORDER BY diary_date ASC")
    List<Map<String, Object>> selectEmotionTrend(@Param("days") int days);



    @Select("<script>" +
        "SELECT ed.*, u.username, u.nickname FROM emotion_diary ed " +
        "LEFT JOIN user u ON ed.user_id = u.id " +
        "<where>" +
        "<if test='params.userId != null'>AND ed.user_id = #{params.userId}</if>" +
        "<if test='params.moodScoreMin != null and params.moodScoreMax != null'>AND ed.mood_score BETWEEN #{params.moodScoreMin} AND #{params.moodScoreMax}</if>" +
        "</where>" +
        " ORDER BY ed.created_at DESC" +
        "</script>")
    List<EmotionDiaryPageDTO> selectAdminPage(@Param("params") java.util.Map<String, Object> params);

}
