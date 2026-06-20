package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.service.MonitorService;
import com.sparkit.system.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 监控管理
 */
@RestController
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;
    private final OnlineUserService onlineUserService;

    // ============ 在线用户 ============

    @GetMapping("/api/v1/admin/monitor/online-users")
    public R<?> listOnlineUsers() {
        return R.ok(onlineUserService.listOnlineUsers());
    }

    @GetMapping("/api/v1/admin/monitor/online-count")
    public R<Map<String, Long>> onlineCount() {
        return R.ok(Map.of("count", onlineUserService.countOnline()));
    }

    @PostMapping("/api/v1/admin/monitor/force-offline/{userId}")
    public R<?> forceOffline(@PathVariable Long userId) {
        onlineUserService.forceOffline(userId);
        return R.ok();
    }

    // ============ 系统监控 ============

    @GetMapping("/api/v1/admin/monitor/metrics")
    public R<Map<String, Object>> getAllMetrics() {
        return R.ok(monitorService.getAllMetrics());
    }

    @GetMapping("/api/v1/admin/monitor/cpu")
    public R<Map<String, Object>> getCpuInfo() {
        return R.ok(monitorService.getCpuInfo());
    }

    @GetMapping("/api/v1/admin/monitor/memory")
    public R<Map<String, Object>> getMemoryInfo() {
        return R.ok(monitorService.getMemoryInfo());
    }

    @GetMapping("/api/v1/admin/monitor/jvm")
    public R<Map<String, Object>> getJvmInfo() {
        return R.ok(monitorService.getJvmInfo());
    }

    @GetMapping("/api/v1/admin/monitor/disk")
    public R<Map<String, Object>> getDiskInfo() {
        return R.ok(monitorService.getDiskInfo());
    }

    @GetMapping("/api/v1/admin/monitor/db")
    public R<Map<String, Object>> getDbInfo() {
        return R.ok(monitorService.getDbInfo());
    }
}