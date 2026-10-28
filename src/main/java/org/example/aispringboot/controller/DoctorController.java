package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.Appointment;
import org.example.aispringboot.entity.ConsultationSummary;
import org.example.aispringboot.entity.Counselor;
import org.example.aispringboot.mapper.AppointmentMapper;
import org.example.aispringboot.mapper.CounselorMapper;
import org.example.aispringboot.service.AppointmentService;
import org.example.aispringboot.service.ConsultationSummaryService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    @Resource
    private CounselorMapper counselorMapper;

    @Resource
    private AppointmentService appointmentService;

    @Resource
    private ConsultationSummaryService summaryService;

    @Resource
    private AppointmentMapper appointmentMapper;

    private Counselor getCurrentCounselor() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        Counselor counselor = counselorMapper.getByUserId(userId);
        if (counselor == null) {
            throw new RuntimeException("当前用户未关联医生信息");
        }
        return counselor;
    }

    @GetMapping("/appointments/today")
    public Result<List<Appointment>> todayAppointments() {
        Counselor counselor = getCurrentCounselor();
        List<Appointment> list = appointmentService.getTodayAppointments(counselor.getId());
        return Result.ok(list);
    }

    @GetMapping("/appointments")
    public Result<IPage<Appointment>> doctorAppointments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer status) {
        Counselor counselor = getCurrentCounselor();
        Map<String, Object> params = new HashMap<>();
        if (dateFrom != null && !dateFrom.isEmpty()) params.put("dateFrom", dateFrom);
        if (dateTo != null && !dateTo.isEmpty()) params.put("dateTo", dateTo);
        if (status != null) params.put("status", status);
        IPage<Appointment> result = appointmentService.getDoctorAppointments(
                counselor.getId(), pageNum, pageSize, params);
        return Result.ok(result);
    }

    @PutMapping("/appointment/{id}/complete")
    public Result<Void> completeAppointment(@PathVariable Long id) {
        Counselor counselor = getCurrentCounselor();
        try {
            appointmentService.completeAppointment(id, counselor.getId());
        } catch (RuntimeException e) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage(), null);
        }
        return Result.ok();
    }
    @GetMapping("/patient/{userId}/summaries")
    public Result<List<ConsultationSummary>> getPatientSummaries(@PathVariable Long userId) {
        Counselor counselor = getCurrentCounselor();
        Long count = appointmentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getUserId, userId)
                        .eq(Appointment::getCounselorId, counselor.getId()));
        if (count == 0) {
            return Result.error(ResultCode.AUTHORIZED_ERROR.getCode(), "无权限查看该患者", null);
        }
        List<ConsultationSummary> summaries = summaryService.getPatientSummaries(userId);
        return Result.ok(summaries);
    }
}
