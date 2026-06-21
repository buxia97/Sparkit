package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 仪表盘
 */
@Tag(name = "仪表盘", description = "首页仪表盘数据统计")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取仪表盘数据")
    @GetMapping
    public R<Map<String, Object>> dashboard() {
        return R.ok(dashboardService.getDashboard());
    }
}