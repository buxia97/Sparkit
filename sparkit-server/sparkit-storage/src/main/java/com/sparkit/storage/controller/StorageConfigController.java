package com.sparkit.storage.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.storage.model.entity.StorageConfig;
import com.sparkit.storage.service.StorageConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存储源配置管理
 */
@Tag(name = "存储源配置", description = "存储源的增删改查、设为默认")
@RestController
@RequestMapping("/api/v1/admin/storage")
@RequiredArgsConstructor
public class StorageConfigController {

    private final StorageConfigService configService;

    /** 存储源列表 */
    @Operation(summary = "存储源列表")
    @GetMapping("/configs")
    public R<PageResult<StorageConfig>> list(PageQuery query) {
        return R.ok(configService.page(query));
    }

    /** 全部存储源（下拉选择用） */
    @Operation(summary = "全部存储源")
    @GetMapping("/configs/all")
    public R<List<StorageConfig>> all() {
        return R.ok(configService.list());
    }

    /** 获取单个存储源 */
    @Operation(summary = "获取存储源详情")
    @GetMapping("/configs/{id}")
    public R<StorageConfig> getById(@PathVariable Long id) {
        return R.ok(configService.getById(id));
    }

    /** 新增存储源 */
    @Operation(summary = "新增存储源")
    @PostMapping("/configs")
    public R<?> create(@RequestBody StorageConfig config) {
        configService.save(config);
        return R.ok();
    }

    /** 更新存储源 */
    @Operation(summary = "更新存储源")
    @PutMapping("/configs/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody StorageConfig config) {
        config.setId(id);
        configService.updateById(config);
        return R.ok();
    }

    /** 删除存储源 */
    @Operation(summary = "删除存储源")
    @DeleteMapping("/configs/{id}")
    public R<?> delete(@PathVariable Long id) {
        configService.removeById(id);
        return R.ok();
    }

    /** 切换默认存储源 */
    @Operation(summary = "设为默认存储源")
    @PutMapping("/configs/{id}/default")
    public R<?> setDefault(@PathVariable Long id) {
        StorageConfig config = configService.getById(id);
        if (config != null) {
            config.setIsDefault(1);
            configService.updateById(config);
        }
        return R.ok();
    }
}