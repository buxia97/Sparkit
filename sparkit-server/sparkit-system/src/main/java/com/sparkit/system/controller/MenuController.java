package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Menu;
import com.sparkit.system.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理
 */
@RestController
@RequestMapping("/api/v1/admin/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public R<List<Menu>> tree() {
        return R.ok(menuService.getMenuTree());
    }

    @GetMapping("/{id}")
    public R<Menu> getById(@PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @PostMapping
    public R<?> create(@Valid @RequestBody Menu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Menu menu) {
        menu.setId(id);
        menuService.update(menu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}