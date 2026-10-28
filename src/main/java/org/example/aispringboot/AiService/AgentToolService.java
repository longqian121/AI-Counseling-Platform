package org.example.aispringboot.AiService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.entity.Appointment;
import org.example.aispringboot.entity.Counselor;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.mapper.AppointmentMapper;
import org.example.aispringboot.mapper.CounselorMapper;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentToolService {

    @Resource
    private CounselorMapper counselorMapper;

    @Resource
    private AppointmentMapper appointmentMapper;

    @Resource
    private consultationSessionMapper consultationSessionMapper;

    @Resource
    private PreVisitBriefingService preVisitBriefingService;

    private static final String[] DEFAULT_TIME_SLOTS = {
        "09:00-10:00", "10:00-11:00", "11:00-12:00",
        "14:00-15:00", "15:00-16:00", "16:00-17:00"
    };

    private static final int MAX_BOOKING_DAYS = 14;

    @Tool(description = "查询所有可预约的心理医生列表")
    public String listCounselors() {
        List<Counselor> counselors = counselorMapper.selectList(
                new LambdaQueryWrapper<Counselor>().eq(Counselor::getStatus, 1));
        if (counselors.isEmpty()) {
            return "当前没有可预约的心理医生。";
        }
        StringBuilder sb = new StringBuilder("可预约的心理医生：\n");
        for (Counselor c : counselors) {
            sb.append(String.format("- ID:%d %s %s 擅长:%s\n",
                    c.getId(), c.getName(), c.getTitle(), c.getSpecialty()));
        }
        return sb.toString();
    }

    @Tool(description = "查询某位心理医生在某天的可预约时间段，需要医生ID和日期(格式yyyy-MM-dd)，日期仅限今天起两周内（含今天）")
    public String getAvailableSlots(
            @ToolParam(description = "心理医生ID") Long counselorId,
            @ToolParam(description = "日期，格式yyyy-MM-dd，必须是今天到两周内") String date) {
        Counselor counselor = counselorMapper.selectById(counselorId);
        if (counselor == null || counselor.getStatus() != 1) {
            return "该医生不存在或当前不可预约。";
        }

        LocalDate today = LocalDate.now();
        LocalDate queryDate = LocalDate.parse(date);
        LocalDate maxDate = today.plusDays(MAX_BOOKING_DAYS);
        if (queryDate.isBefore(today)) {
            return "不能查询过去的日期，请指定今天或之后的日期。";
        }
        if (queryDate.isAfter(maxDate)) {
            return String.format("仅支持查看%s至%s之间的可预约时段。", today, maxDate);
        }

        List<Appointment> booked = appointmentMapper.findByCounselorIdAndDate(counselorId, date);
        List<String> bookedSlots = booked.stream()
                .map(Appointment::getTimeSlot)
                .collect(Collectors.toList());

        List<String> available = new ArrayList<>();
        for (String slot : DEFAULT_TIME_SLOTS) {
            if (!bookedSlots.contains(slot)) {
                available.add(slot);
            }
        }

        if (available.isEmpty()) {
            return String.format("%s医生在%s当天已全部约满。", counselor.getName(), date);
        }

        return String.format("%s医生在%s的可预约时段：%s",
                counselor.getName(), date, String.join("、", available));
    }

    @Tool(description = "为用户预约心理咨询。系统会自动识别当前用户，只需提供医生ID、日期(yyyy-MM-dd)、时间段(如14:00-15:00)、备注(可选)，日期仅限今天起两周内（含今天）")
    public String bookAppointment(
            @ToolParam(description = "心理医生ID") Long counselorId,
            @ToolParam(description = "预约日期，格式yyyy-MM-dd，必须是今天到两周内") String date,
            @ToolParam(description = "时间段，如14:00-15:00") String timeSlot,
            @ToolParam(description = "用户备注，可选") String remark,
            ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) {
            return "预约失败：无法识别当前用户，请先登录。";
        }

        Counselor counselor = counselorMapper.selectById(counselorId);
        if (counselor == null || counselor.getStatus() != 1) {
            return "预约失败：该医生不存在或当前不可预约。";
        }

        boolean validSlot = false;
        for (String s : DEFAULT_TIME_SLOTS) {
            if (s.equals(timeSlot)) {
                validSlot = true;
                break;
            }
        }
        if (!validSlot) {
            return "预约失败：时间段无效，可选时段为 " + String.join("、", DEFAULT_TIME_SLOTS);
        }

        LocalDate today = LocalDate.now();
        LocalDate bookingDate = LocalDate.parse(date);
        LocalDate maxDate = today.plusDays(MAX_BOOKING_DAYS);
        if (bookingDate.isBefore(today)) {
            return "预约失败：不能预约过去的日期。";
        }
        if (bookingDate.isAfter(maxDate)) {
            return String.format("预约失败：仅支持预约%s至%s之间的日期。", today, maxDate);
        }

        List<Appointment> booked = appointmentMapper.findByCounselorIdAndDate(counselorId, date);
        for (Appointment a : booked) {
            if (a.getTimeSlot().equals(timeSlot)) {
                return "预约失败：该时段已被预约，请选择其他时段。";
            }
        }

        Appointment appointment = Appointment.builder()
                .userId(userId)
                .counselorId(counselorId)
                .appointmentDate(LocalDate.parse(date))
                .timeSlot(timeSlot)
                .status(1)
                .remark(remark)
                .createdAt(LocalDateTime.now())
                .build();
        appointmentMapper.insert(appointment);
        preVisitBriefingService.generateBriefingAsync(appointment);

        return String.format("预约成功！%s医生，%s %s。请准时参加。", counselor.getName(), date, timeSlot);
    }

    @Tool(description = "取消已有的心理咨询预约，需要预约ID和取消原因")
    public String cancelAppointment(
            @ToolParam(description = "预约ID") Long appointmentId,
            @ToolParam(description = "取消原因") String reason) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            return "取消失败：未找到该预约记录。";
        }
        if (appointment.getStatus() == 0) {
            return "该预约已经取消过了。";
        }

        appointment.setStatus(0);
        appointment.setCancelReason(reason);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        return "预约已取消。";
    }

    @Tool(description = "查询当前用户的预约记录，无需提供用户ID，系统自动识别")
    public String getMyAppointments(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) {
            return "无法识别当前用户，请先登录。";
        }

        List<Appointment> appointments = appointmentMapper.selectList(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getUserId, userId)
                        .orderByDesc(Appointment::getAppointmentDate));

        if (appointments.isEmpty()) {
            return "您目前没有预约记录。";
        }

        StringBuilder sb = new StringBuilder("您的预约记录：\n");
        for (Appointment a : appointments) {
            Counselor c = counselorMapper.selectById(a.getCounselorId());
            String counselorName = c != null ? c.getName() : "未知医生";
            String statusText = switch (a.getStatus()) {
                case 0 -> "已取消";
                case 1 -> "已预约";
                case 2 -> "已完成";
                default -> "未知";
            };
            sb.append(String.format("- ID:%d %s %s %s %s\n",
                    a.getId(), counselorName, a.getAppointmentDate(), a.getTimeSlot(), statusText));
        }
        return sb.toString();
    }

    @Tool(description = "查询用户的历史咨询会话列表，需要用户ID")
    public String getConsultationHistory(@ToolParam(description = "用户ID") Long userId) {
        List<ConsultationSession> sessions = consultationSessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>()
                        .eq(ConsultationSession::getUserId, userId)
                        .orderByDesc(ConsultationSession::getStartedAt)
                        .last("LIMIT 10"));

        if (sessions.isEmpty()) {
            return "您目前没有咨询记录。";
        }

        StringBuilder sb = new StringBuilder("您的最近咨询记录：\n");
        for (ConsultationSession s : sessions) {
            sb.append(String.format("- ID:%d %s %s\n",
                    s.getId(), s.getSessionTitle(),
                    s.getStartedAt() != null ? s.getStartedAt().toLocalDate().toString() : ""));
        }
        return sb.toString();
    }

    @Tool(description = "查询指定会话的对话记录，需要会话ID")
    public String getSessionMessages(@ToolParam(description = "会话ID") Long sessionId) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            return "未找到该会话。";
        }
        return String.format("会话「%s」的ID为%d，可通过前端页面查看完整对话记录。",
                session.getSessionTitle(), session.getId());
    }
}
