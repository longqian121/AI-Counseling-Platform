package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.aispringboot.entity.Appointment;

import java.util.List;
import java.util.Map;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    @Select("SELECT a.*, c.name AS counselorName, c.title AS counselorTitle, u.nickname AS userNickname " +
            "FROM appointment a " +
            "LEFT JOIN counselor c ON a.counselor_id = c.id " +
            "LEFT JOIN user u ON a.user_id = u.id " +
            "ORDER BY a.appointment_date DESC, a.time_slot ASC")
    IPage<Appointment> selectAppointmentPage(Page<?> page, @Param("params") java.util.Map<String, Object> params);

    @Select("<script>" +
            "SELECT a.*, c.name AS counselorName, c.title AS counselorTitle, u.nickname AS userNickname " +
            "FROM appointment a " +
            "LEFT JOIN counselor c ON a.counselor_id = c.id " +
            "LEFT JOIN user u ON a.user_id = u.id " +
        "<where>" +
        "<if test='params.userId != null'>AND a.user_id = #{params.userId}</if> " +
        "<if test='params.counselorId != null'>AND a.counselor_id = #{params.counselorId}</if> " +
        "<if test='params.status != null'>AND a.status = #{params.status}</if> " +
        "</where>" +
            " ORDER BY a.appointment_date DESC, a.time_slot ASC" +
            "</script>")
    IPage<Appointment> selectPageWithParams(Page<?> page, @Param("params") java.util.Map<String, Object> params);

    @Select("SELECT * FROM appointment WHERE counselor_id = #{counselorId} AND appointment_date = #{date} AND status = 1")
    List<Appointment> findByCounselorIdAndDate(@Param("counselorId") Long counselorId,
                                                @Param("date") String date);

    @Select("<script>" +
            "SELECT a.*, u.nickname AS userNickname FROM appointment a " +
            "LEFT JOIN user u ON a.user_id = u.id " +
            "WHERE a.counselor_id = #{counselorId} " +
            "<if test='params.dateFrom != null and params.dateFrom != \"\"'>AND a.appointment_date &gt;= #{params.dateFrom}</if> " +
            "<if test='params.dateTo != null and params.dateTo != \"\"'>AND a.appointment_date &lt;= #{params.dateTo}</if> " +
            "<if test='params.status != null'>AND a.status = #{params.status}</if> " +
            "ORDER BY a.appointment_date DESC, a.time_slot ASC" +
            "</script>")
    IPage<Appointment> getDoctorAppointments(Page<?> page,
            @Param("counselorId") Long counselorId,
            @Param("params") Map<String, Object> params);

    @Select("<script>" +
            "SELECT a.*, u.nickname AS userNickname FROM appointment a " +
            "LEFT JOIN user u ON a.user_id = u.id " +
            "WHERE a.counselor_id = #{counselorId} " +
            "AND a.appointment_date = CURDATE() " +
            "AND a.status = 1 " +
            "ORDER BY a.time_slot ASC" +
            "</script>")
    List<Appointment> selectTodayByCounselorId(@Param("counselorId") Long counselorId);
}
