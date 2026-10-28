package org.example.aispringboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.ConsultationSummary;
import org.example.aispringboot.mapper.ConsultationSummaryMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/consultation")
public class AdminConsultationSummaryController {

    @Resource
    private ConsultationSummaryMapper summaryMapper;

    @GetMapping("/summary/page")
    public Result<IPage<ConsultationSummary>> page(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        Page<ConsultationSummary> page = new Page<>(currentPage, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsultationSummary> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(ConsultationSummary::getUserId, userId);
        }
        wrapper.orderByDesc(ConsultationSummary::getCreatedAt);
        IPage<ConsultationSummary> result = summaryMapper.selectPage(page, wrapper);
        return Result.ok(result);
    }

    @GetMapping("/summary/{id}")
    public Result<ConsultationSummary> getById(@PathVariable Long id) {
        ConsultationSummary summary = summaryMapper.selectById(id);
        if (summary == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "总结不存在", null);
        }
        return Result.ok(summary);
    }
}
