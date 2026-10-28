package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.Counselor;
import org.example.aispringboot.entity.PreVisitBriefing;
import org.example.aispringboot.mapper.CounselorMapper;
import org.example.aispringboot.mapper.PreVisitBriefingMapper;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
public class PreVisitBriefingController {

    @Resource
    private PreVisitBriefingMapper briefingMapper;

    @Resource
    private CounselorMapper counselorMapper;

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

    @GetMapping("/briefing/{appointmentId}")
    public Result<PreVisitBriefing> getBriefing(@PathVariable Long appointmentId) {
        Counselor counselor = getCurrentCounselor();
        PreVisitBriefing briefing = briefingMapper.selectOne(
                new LambdaQueryWrapper<PreVisitBriefing>()
                        .eq(PreVisitBriefing::getAppointmentId, appointmentId)
                        .eq(PreVisitBriefing::getDoctorUserId, counselor.getId()));
        if (briefing == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "简报不存在或无权查看", null);
        }
        // 标记已读
        if (briefing.getIsViewed() == null || briefing.getIsViewed() == 0) {
            briefing.setIsViewed(1);
            briefingMapper.updateById(briefing);
        }
        return Result.ok(briefing);
    }

    @GetMapping("/briefing/my-patient/{userId}")
    public Result<PreVisitBriefing> getLatestBriefing(@PathVariable Long userId) {
        Counselor counselor = getCurrentCounselor();
        PreVisitBriefing briefing = briefingMapper.selectOne(
                new LambdaQueryWrapper<PreVisitBriefing>()
                        .eq(PreVisitBriefing::getPatientUserId, userId)
                        .eq(PreVisitBriefing::getDoctorUserId, counselor.getId())
                        .orderByDesc(PreVisitBriefing::getCreatedAt)
                        .last("LIMIT 1"));
        if (briefing == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "暂无该患者的简报", null);
        }
        return Result.ok(briefing);
    }
}
