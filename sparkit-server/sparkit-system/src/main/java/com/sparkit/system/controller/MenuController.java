package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Menu;
import com.sparkit.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理
 */
@Tag(name = "菜单管理", description = "菜单树的增删改查")
@RestController
@RequestMapping("/api/v1/admin/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    public R<List<Menu>> tree() {
        return R.ok(menuService.getMenuTree());
    }

    @Operation(summary = "获取菜单详情")
    @GetMapping("/{id}")
    public R<Menu> getById(@PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @Operation(summary = "创建菜单")
    @PostMapping
    public R<?> create(@Valid @RequestBody Menu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Menu menu) {
        menu.setId(id);
        menuService.update(menu);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}