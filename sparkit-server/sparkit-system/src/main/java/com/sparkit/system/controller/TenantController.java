package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Tenant;
import com.sparkit.system.model.entity.TenantPackage;
import com.sparkit.system.service.TenantPackageService;
import com.sparkit.system.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理
 */
@Tag(name = "租户管理", description = "租户与套餐的增删改查")
@RestController
@RequestMapping("/api/v1/admin/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final TenantPackageService packageService;

    // ============ 租户管理 ============

    @Operation(summary = "租户列表")
    @GetMapping
    public R<PageResult<Tenant>> list(PageQuery query) {
        return R.ok(tenantService.page(query));
    }

    @Operation(summary = "获取租户详情")
    @GetMapping("/{id}")
    public R<Tenant> getById(@PathVariable Long id) {
        return R.ok(tenantService.getById(id));
    }

    @Operation(summary = "创建租户")
    @PostMapping
    public R<?> create(@Valid @RequestBody Tenant tenant) {
        tenantService.create(tenant);
        return R.ok();
    }

    @Operation(summary = "更新租户")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Tenant tenant) {
        tenant.setId(id);
        tenantService.update(tenant);
        return R.ok();
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        tenantService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "修改租户状态")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        tenantService.changeStatus(id, status);
        return R.ok();
    }

    // ============ 套餐管理 ============

    @Operation(summary = "套餐列表")
    @GetMapping("/packages")
    public R<PageResult<TenantPackage>> packageList(PageQuery query) {
        return R.ok(packageService.page(query));
    }

    @Operation(summary = "全部套餐")
    @GetMapping("/packages/all")
    public R<List<TenantPackage>> packageAll() {
        return R.ok(packageService.listEnabled());
    }

    @Operation(summary = "获取套餐详情")
    @GetMapping("/packages/{id}")
    public R<TenantPackage> packageGet(@PathVariable Long id) {
        return R.ok(packageService.getById(id));
    }

    @Operation(summary = "创建套餐")
    @PostMapping("/packages")
    public R<?> packageCreate(@Valid @RequestBody TenantPackage pkg) {
        packageService.create(pkg);
        return R.ok();
    }

    @Operation(summary = "更新套餐")
    @PutMapping("/packages/{id}")
    public R<?> packageUpdate(@PathVariable Long id, @Valid @RequestBody TenantPackage pkg) {
        pkg.setId(id);
        packageService.update(pkg);
        return R.ok();
    }

    @Operation(summary = "删除套餐")
    @DeleteMapping("/packages/{id}")
    public R<?> packageDelete(@PathVariable Long id) {
        packageService.removeById(id);
        return R.ok();
    }
}