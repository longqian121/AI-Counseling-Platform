package org.example.aispringboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.RiskAlert;
import org.example.aispringboot.mapper.RiskAlertMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/risk-alert")
public class RiskAlertController {

    @Resource
    private RiskAlertMapper riskAlertMapper;

    @GetMapping("/page")
    public Result<IPage<RiskAlert>> page(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(required = false) Integer isRead) {
        Page<RiskAlert> page = new Page<>(currentPage, size);
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        if (riskLevel != null) {
            wrapper.eq(RiskAlert::getRiskLevel, riskLevel);
        }
        if (isRead != null) {
            wrapper.eq(RiskAlert::getIsRead, isRead);
        }
        wrapper.orderByDesc(RiskAlert::getCreatedAt);
        IPage<RiskAlert> result = riskAlertMapper.selectPage(page, wrapper);
        return Result.ok(result);
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        RiskAlert alert = riskAlertMapper.selectById(id);
        if (alert != null) {
            alert.setIsRead(1);
            riskAlertMapper.updateById(alert);
        }
        return Result.ok();
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long count = riskAlertMapper.selectCount(
                new LambdaQueryWrapper<RiskAlert>().eq(RiskAlert::getIsRead, 0));
        return Result.ok(count);
    }
}
