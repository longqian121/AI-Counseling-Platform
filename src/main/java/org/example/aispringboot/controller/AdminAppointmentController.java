package org.example.aispringboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.Appointment;
import org.example.aispringboot.mapper.AppointmentMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/appointment")
public class AdminAppointmentController {

    @Resource
    private AppointmentMapper appointmentMapper;

    @GetMapping("/page")
    public Result<IPage<Appointment>> page(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long counselorId,
            @RequestParam(required = false) Integer status) {
        Page<Appointment> page = new Page<>(currentPage, size);
        Map<String, Object> params = new HashMap<>();
        if (userId != null) params.put("userId", userId);
        if (counselorId != null) params.put("counselorId", counselorId);
        if (status != null) params.put("status", status);
        IPage<Appointment> result = appointmentMapper.selectPageWithParams(page, params);
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<Appointment> getById(@PathVariable Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        return Result.ok(appointment);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment != null) {
            appointment.setStatus(status);
            appointmentMapper.updateById(appointment);
        }
        return Result.ok();
    }
}
