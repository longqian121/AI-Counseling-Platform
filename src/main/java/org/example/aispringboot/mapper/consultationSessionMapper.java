package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.DTO.response.SessionListResponseDTO;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Mapper
public interface consultationSessionMapper extends BaseMapper<ConsultationSession> {

    @Select("SELECT cs.id, cs.session_title AS sessionTitle, " +
            "u.nickname AS userNickname, " +
            "(SELECT content FROM consultation_message WHERE session_id = cs.id ORDER BY created_at DESC LIMIT 1) AS lastMessageContent, " +
            "(SELECT created_at FROM consultation_message WHERE session_id = cs.id ORDER BY created_at DESC LIMIT 1) AS lastMessageTime, " +
            "(SELECT COUNT(*) FROM consultation_message WHERE session_id = cs.id) AS messageCount " +
            "FROM consultation_session cs " +
            "LEFT JOIN user u ON cs.user_id = u.id " +
            "WHERE cs.user_id = #{userId} " +
            "ORDER BY cs.started_at DESC")
    IPage<SessionListResponseDTO> selectSessionPage(Page<?> page, @Param("userId") Long userId);

    @Select("SELECT DATE(started_at) AS date, COUNT(*) AS sessionCount FROM consultation_session WHERE started_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(started_at) ORDER BY date ASC")
    List<Map<String, Object>> selectDailyTrend(@Param("days") int days);



    @Select("<script>" +
        "SELECT cs.id, cs.user_id AS userId, cs.session_title AS sessionTitle, cs.started_at AS startedAt, " +
        "u.nickname AS userNickname, " +
        "(SELECT content FROM consultation_message WHERE session_id = cs.id ORDER BY created_at DESC LIMIT 1) AS lastMessageContent, " +
        "(SELECT created_at FROM consultation_message WHERE session_id = cs.id ORDER BY created_at DESC LIMIT 1) AS lastMessageTime, " +
        "(SELECT COUNT(*) FROM consultation_message WHERE session_id = cs.id) AS messageCount " +
        "FROM consultation_session cs " +
        "LEFT JOIN user u ON cs.user_id = u.id " +
        "<where>" +
        "<if test='params.userId != null'>AND cs.user_id = #{params.userId}</if>" +
        "<if test='params.sessionId != null'>AND cs.id = #{params.sessionId}</if>" +
        "</where>" +
        " ORDER BY cs.started_at DESC" +
        "</script>")
    IPage<SessionListResponseDTO> selectAdminSessionPage(Page<?> page, @Param("params") java.util.Map<String, Object> params);



    @Select("SELECT DATE(started_at) AS date, COUNT(DISTINCT user_id) AS userCount FROM consultation_session WHERE started_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(started_at) ORDER BY date ASC")
    List<Map<String, Object>> selectDailyUserCount(@Param("days") int days);
}
