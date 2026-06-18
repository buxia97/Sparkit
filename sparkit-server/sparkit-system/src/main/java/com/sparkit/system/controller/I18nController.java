package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.I18n;
import com.sparkit.system.service.I18nService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 国际化管理
 */
@RestController
@RequestMapping("/api/v1/admin/i18n")
@RequiredArgsConstructor
public class I18nController {

    private final I18nService i18nService;

    @GetMapping("/{lang}")
    public R<Map<String, String>> get(@PathVariable String lang) {
        return R.ok(i18nService.getI18nMap(lang));
    }

    @PostMapping
    public R<?> save(@RequestBody I18n i18n) {
        i18nService.save(i18n);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody I18n i18n) {
        i18n.setId(id);
        i18nService.updateById(i18n);
        return R.ok();
    }
}