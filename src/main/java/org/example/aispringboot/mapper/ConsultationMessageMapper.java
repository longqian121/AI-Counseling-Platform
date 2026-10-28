package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.ConsultationMessage;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConsultationMessageMapper extends BaseMapper<ConsultationMessage> {

    @Select("SELECT session_id, MIN(created_at) AS firstTime, MAX(created_at) AS lastTime FROM consultation_message WHERE session_id IS NOT NULL GROUP BY session_id")
    List<Map<String, Object>> selectSessionDurations();

}
