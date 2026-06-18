package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Role;
import com.sparkit.system.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public R<List<Role>> list() {
        return R.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    public R<Role> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @PostMapping
    public R<?> create(@Valid @RequestBody Role role) {
        roleService.create(role);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Role role) {
        role.setId(id);
        roleService.update(role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/menus")
    public R<?> assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        roleService.assignMenus(id, params.get("menuIds"));
        return R.ok();
    }
}