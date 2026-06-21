package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.service.MonitorService;
import com.sparkit.system.service.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 监控管理
 */
@Tag(name = "监控管理", description = "在线用户、系统指标监控")
@RestController
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;
    private final OnlineUserService onlineUserService;

    // ============ 在线用户 ============

    @Operation(summary = "在线用户列表")
    @GetMapping("/api/v1/admin/monitor/online-users")
    public R<?> listOnlineUsers() {
        return R.ok(onlineUserService.listOnlineUsers());
    }

    @Operation(summary = "在线用户数量")
    @GetMapping("/api/v1/admin/monitor/online-count")
    public R<Map<String, Long>> onlineCount() {
        return R.ok(Map.of("count", onlineUserService.countOnline()));
    }

    @Operation(summary = "强制用户下线")
    @PostMapping("/api/v1/admin/monitor/force-offline/{userId}")
    public R<?> forceOffline(@PathVariable Long userId) {
        onlineUserService.forceOffline(userId);
        return R.ok();
    }

    // ============ 系统监控 ============

    @Operation(summary = "获取所有系统指标")
    @GetMapping("/api/v1/admin/monitor/metrics")
    public R<Map<String, Object>> getAllMetrics() {
        return R.ok(monitorService.getAllMetrics());
    }

    @Operation(summary = "CPU 信息")
    @GetMapping("/api/v1/admin/monitor/cpu")
    public R<Map<String, Object>> getCpuInfo() {
        return R.ok(monitorService.getCpuInfo());
    }

    @Operation(summary = "内存信息")
    @GetMapping("/api/v1/admin/monitor/memory")
    public R<Map<String, Object>> getMemoryInfo() {
        return R.ok(monitorService.getMemoryInfo());
    }

    @Operation(summary = "JVM 信息")
    @GetMapping("/api/v1/admin/monitor/jvm")
    public R<Map<String, Object>> getJvmInfo() {
        return R.ok(monitorService.getJvmInfo());
    }

    @Operation(summary = "磁盘信息")
    @GetMapping("/api/v1/admin/monitor/disk")
    public R<Map<String, Object>> getDiskInfo() {
        return R.ok(monitorService.getDiskInfo());
    }

    @Operation(summary = "数据库连接池信息")
    @GetMapping("/api/v1/admin/monitor/db")
    public R<Map<String, Object>> getDbInfo() {
        return R.ok(monitorService.getDbInfo());
    }
}