package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Config;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统配置管理
 */
@RestController
@RequestMapping("/api/v1/admin/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public R<Map<String, String>> list() {
        return R.ok(configService.getConfigByGroup("system"));
    }

    @GetMapping("/group/{group}")
    public R<Map<String, String>> listByGroup(@PathVariable String group) {
        return R.ok(configService.getConfigByGroup(group));
    }

    @GetMapping("/key/{key}")
    public R<String> getValue(@PathVariable String key) {
        return R.ok(configService.getConfigValue(key));
    }

    @PutMapping("/batch")
    public R<?> batchSave(@RequestBody Map<String, Object> params) {
        String group = (String) params.get("group");
        @SuppressWarnings("unchecked")
        Map<String, String> configs = (Map<String, String>) params.get("configs");
        configService.batchSave(group, configs);
        return R.ok();
    }
}