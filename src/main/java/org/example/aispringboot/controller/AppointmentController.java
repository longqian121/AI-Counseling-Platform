package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.Appointment;
import org.example.aispringboot.mapper.AppointmentMapper;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    @Resource
    private AppointmentMapper appointmentMapper;

    @GetMapping("/my")
    public Result<IPage<Appointment>> myAppointments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        Page<Appointment> page = new Page<>(pageNum, pageSize);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        IPage<Appointment> result = appointmentMapper.selectPageWithParams(page, params);
        return Result.ok(result);
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancelAppointment(@PathVariable Long id) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "预约不存在", null);
        }
        if (!appointment.getUserId().equals(userId)) {
            return Result.error(ResultCode.AUTHORIZED_ERROR.getCode(), "无权操作该预约", null);
        }
        if (appointment.getStatus() != 1) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "仅已预约状态可取消", null);
        }

        appointment.setStatus(0);
        appointment.setCancelReason("用户主动取消");
        appointmentMapper.updateById(appointment);
        return Result.ok();
    }
}
