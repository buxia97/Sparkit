package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Region;
import com.sparkit.system.service.RegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地区管理
 */
@RestController
@RequestMapping("/api/v1/admin/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/tree")
    public R<List<Region>> tree() {
        return R.ok(regionService.getRegionTree());
    }

    @GetMapping("/children/{parentCode}")
    public R<List<Region>> children(@PathVariable String parentCode) {
        return R.ok(regionService.getByParentCode(parentCode));
    }

    @GetMapping
    public R<List<Region>> list() {
        return R.ok(regionService.list());
    }

    @GetMapping("/{id}")
    public R<Region> get(@PathVariable Long id) {
        return R.ok(regionService.getById(id));
    }

    @PostMapping
    public R<?> create(@Valid @RequestBody Region region) {
        regionService.save(region);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Region region) {
        region.setId(id);
        regionService.updateById(region);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        regionService.removeById(id);
        return R.ok();
    }
}