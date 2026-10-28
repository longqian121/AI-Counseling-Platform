package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.aispringboot.entity.Counselor;

@Mapper
public interface CounselorMapper extends BaseMapper<Counselor> {

    @Select("SELECT * FROM counselor WHERE user_id = #{userId}")
    Counselor getByUserId(@Param("userId") Long userId);
}
