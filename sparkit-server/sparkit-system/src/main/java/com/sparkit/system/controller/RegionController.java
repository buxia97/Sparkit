package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Region;
import com.sparkit.system.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地区管理
 */
@Tag(name = "地区管理", description = "地区树的增删改查")
@RestController
@RequestMapping("/api/v1/admin/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "获取地区树")
    @GetMapping("/tree")
    public R<List<Region>> tree() {
        return R.ok(regionService.getRegionTree());
    }

    @Operation(summary = "根据父级编码获取子地区")
    @GetMapping("/children/{parentCode}")
    public R<List<Region>> children(@PathVariable String parentCode) {
        return R.ok(regionService.getByParentCode(parentCode));
    }

    @Operation(summary = "地区列表")
    @GetMapping
    public R<List<Region>> list() {
        return R.ok(regionService.list());
    }

    @Operation(summary = "获取地区详情")
    @GetMapping("/{id}")
    public R<Region> get(@PathVariable Long id) {
        return R.ok(regionService.getById(id));
    }

    @Operation(summary = "创建地区")
    @PostMapping
    public R<?> create(@Valid @RequestBody Region region) {
        regionService.save(region);
        return R.ok();
    }

    @Operation(summary = "更新地区")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Region region) {
        region.setId(id);
        regionService.updateById(region);
        return R.ok();
    }

    @Operation(summary = "删除地区")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        regionService.removeById(id);
        return R.ok();
    }
}