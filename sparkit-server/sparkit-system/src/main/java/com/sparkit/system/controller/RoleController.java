package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Role;
import com.sparkit.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理
 */
@Tag(name = "角色管理", description = "角色的增删改查与菜单分配")
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表")
    @GetMapping
    public R<List<Role>> list() {
        return R.ok(roleService.listAll());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public R<Role> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public R<?> create(@Valid @RequestBody Role role) {
        roleService.create(role);
        return R.ok();
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Role role) {
        role.setId(id);
        roleService.update(role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @PutMapping("/{id}/menus")
    public R<?> assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        roleService.assignMenus(id, params.get("menuIds"));
        return R.ok();
    }
}