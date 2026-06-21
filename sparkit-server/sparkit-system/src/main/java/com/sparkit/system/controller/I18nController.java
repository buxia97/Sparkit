package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.I18n;
import com.sparkit.system.service.I18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 国际化管理
 */
@Tag(name = "国际化管理", description = "国际化翻译的增删改查")
@RestController
@RequestMapping("/api/v1/admin/i18n")
@RequiredArgsConstructor
public class I18nController {

    private final I18nService i18nService;

    @Operation(summary = "获取翻译列表")
    @GetMapping("/{lang}")
    public R<Map<String, String>> get(@PathVariable String lang) {
        return R.ok(i18nService.getI18nMap(lang));
    }

    @Operation(summary = "创建翻译")
    @PostMapping
    public R<?> save(@RequestBody I18n i18n) {
        i18nService.save(i18n);
        return R.ok();
    }

    @Operation(summary = "更新翻译")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody I18n i18n) {
        i18n.setId(id);
        i18nService.updateById(i18n);
        return R.ok();
    }
}