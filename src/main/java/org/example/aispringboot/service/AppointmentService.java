package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispringboot.AiService.PreVisitBriefingService;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.Appointment;
import org.example.aispringboot.mapper.AppointmentMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AppointmentService {

    @Resource
    private AppointmentMapper appointmentMapper;

    @Resource
    private PreVisitBriefingService preVisitBriefingService;

    public List<Appointment> getTodayAppointments(Long counselorId) {
        return appointmentMapper.selectTodayByCounselorId(counselorId);
    }

    public IPage<Appointment> getDoctorAppointments(Long counselorId, int pageNum, int pageSize,
                                                     Map<String, Object> params) {
        Page<Appointment> page = new Page<>(pageNum, pageSize);
        return appointmentMapper.getDoctorAppointments(page, counselorId, params);
    }

    public void completeAppointment(Long id, Long counselorId) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!appointment.getCounselorId().equals(counselorId)) {
            throw new RuntimeException("无权操作该预约");
        }
        if (appointment.getStatus() != 1) {
            throw new RuntimeException("仅已预约状态可标记完成");
        }
        appointment.setStatus(2);
        preVisitBriefingService.generateBriefingAsync(appointment);
        appointmentMapper.updateById(appointment);
    }
}
