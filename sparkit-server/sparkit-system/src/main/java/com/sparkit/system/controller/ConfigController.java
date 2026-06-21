package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Config;
import com.sparkit.system.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统配置管理
 */
@Tag(name = "系统配置", description = "系统配置的查询与批量保存")
@RestController
@RequestMapping("/api/v1/admin/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "获取系统配置列表")
    @GetMapping
    public R<Map<String, String>> list() {
        return R.ok(configService.getConfigByGroup("system"));
    }

    @Operation(summary = "按分组获取配置")
    @GetMapping("/group/{group}")
    public R<Map<String, String>> listByGroup(@PathVariable String group) {
        return R.ok(configService.getConfigByGroup(group));
    }

    @Operation(summary = "获取指定配置值")
    @GetMapping("/key/{key}")
    public R<String> getValue(@PathVariable String key) {
        return R.ok(configService.getConfigValue(key));
    }

    @Operation(summary = "批量保存配置")
    @PutMapping("/batch")
    public R<?> batchSave(@RequestBody Map<String, Object> params) {
        String group = (String) params.get("group");
        @SuppressWarnings("unchecked")
        Map<String, String> configs = (Map<String, String>) params.get("configs");
        configService.batchSave(group, configs);
        return R.ok();
    }
}