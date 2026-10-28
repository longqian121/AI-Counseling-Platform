package org.example.aispringboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.QualityReport;
import org.example.aispringboot.mapper.QualityReportMapper;
import org.example.aispringboot.AiService.QualityInspectionService;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/admin/quality")
public class QualityReportController {

    @Resource
    private QualityReportMapper reportMapper;

    @Resource
    private QualityInspectionService qualityInspectionService;

    @GetMapping("/report/latest")
    public Result<QualityReport> getLatest() {
        QualityReport report = reportMapper.selectOne(
                new LambdaQueryWrapper<QualityReport>()
                        .orderByDesc(QualityReport::getReportDate)
                        .last("LIMIT 1"));
        if (report == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "暂无质检报告", null);
        }
        return Result.ok(report);
    }

    @PostMapping("/report/trigger")
    public Result<String> trigger() {
        CompletableFuture.runAsync(() -> qualityInspectionService.executeDailyInspection());
        return Result.ok("质检任务已触发，运行中...");
    }
}
