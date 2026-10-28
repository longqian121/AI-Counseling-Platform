package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.ConsultationSummary;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.example.aispringboot.service.ConsultationSummaryService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultation")
public class ConsultationSummaryController {

    @Resource
    private ConsultationSummaryService summaryService;

    @Resource
    private consultationSessionMapper sessionMapper;

    @PostMapping("/session/{id}/summary/generate")
    public Result<ConsultationSummary> generateSummary(@PathVariable Long id) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        ConsultationSession session = sessionMapper.selectById(id);
        if (session == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "会话不存在", null);
        }
        if (!session.getUserId().equals(userId)) {
            return Result.error(ResultCode.AUTHORIZED_ERROR.getCode(), "无权操作该会话", null);
        }

        ConsultationSummary summary = summaryService.generateSummary(id);
        return Result.ok(summary);
    }

    @GetMapping("/session/{id}/summary")
    public Result<ConsultationSummary> getSummary(@PathVariable Long id) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        ConsultationSession session = sessionMapper.selectById(id);
        if (session == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "会话不存在", null);
        }
        if (!session.getUserId().equals(userId)) {
            return Result.error(ResultCode.AUTHORIZED_ERROR.getCode(), "无权查看该会话", null);
        }

        ConsultationSummary summary = summaryService.getBySessionId(id);
        return Result.ok(summary);
    }

    @GetMapping("/my-summaries")
    public Result<List<ConsultationSummary>> getMySummaries() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        List<ConsultationSummary> summaries = summaryService.getMySummaries(userId);
        return Result.ok(summaries);
    }
}
