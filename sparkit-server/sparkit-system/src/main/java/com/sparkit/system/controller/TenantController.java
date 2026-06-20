package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Tenant;
import com.sparkit.system.model.entity.TenantPackage;
import com.sparkit.system.service.TenantPackageService;
import com.sparkit.system.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理
 */
@RestController
@RequestMapping("/api/v1/admin/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final TenantPackageService packageService;

    // ============ 租户管理 ============

    @GetMapping
    public R<PageResult<Tenant>> list(PageQuery query) {
        return R.ok(tenantService.page(query));
    }

    @GetMapping("/{id}")
    public R<Tenant> getById(@PathVariable Long id) {
        return R.ok(tenantService.getById(id));
    }

    @PostMapping
    public R<?> create(@Valid @RequestBody Tenant tenant) {
        tenantService.create(tenant);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Tenant tenant) {
        tenant.setId(id);
        tenantService.update(tenant);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        tenantService.removeById(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        tenantService.changeStatus(id, status);
        return R.ok();
    }

    // ============ 套餐管理 ============

    @GetMapping("/packages")
    public R<PageResult<TenantPackage>> packageList(PageQuery query) {
        return R.ok(packageService.page(query));
    }

    @GetMapping("/packages/all")
    public R<List<TenantPackage>> packageAll() {
        return R.ok(packageService.listEnabled());
    }

    @GetMapping("/packages/{id}")
    public R<TenantPackage> packageGet(@PathVariable Long id) {
        return R.ok(packageService.getById(id));
    }

    @PostMapping("/packages")
    public R<?> packageCreate(@Valid @RequestBody TenantPackage pkg) {
        packageService.create(pkg);
        return R.ok();
    }

    @PutMapping("/packages/{id}")
    public R<?> packageUpdate(@PathVariable Long id, @Valid @RequestBody TenantPackage pkg) {
        pkg.setId(id);
        packageService.update(pkg);
        return R.ok();
    }

    @DeleteMapping("/packages/{id}")
    public R<?> packageDelete(@PathVariable Long id) {
        packageService.removeById(id);
        return R.ok();
    }
}