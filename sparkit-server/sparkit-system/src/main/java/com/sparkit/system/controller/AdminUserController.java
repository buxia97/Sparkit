package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.AdminUser;
import com.sparkit.system.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理
 */
@Tag(name = "管理员管理", description = "后台管理员的增删改查、角色分配、登录")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        return R.ok(adminUserService.login(params.get("username"), params.get("password")));
    }

    @Operation(summary = "管理员列表")
    @GetMapping
    public R<PageResult<AdminUser>> list(PageQuery query) {
        return R.ok(adminUserService.page(query));
    }

    @Operation(summary = "获取管理员详情")
    @GetMapping("/{id}")
    public R<AdminUser> getById(@PathVariable Long id) {
        return R.ok(adminUserService.getById(id));
    }

    @Operation(summary = "创建管理员")
    @PostMapping
    public R<?> create(@RequestBody Map<String, Object> params) {
        AdminUser user = parseUser(params);
        adminUserService.create(user);
        if (params.containsKey("roleIds")) {
            @SuppressWarnings("unchecked")
            List<Number> roleIdNums = (List<Number>) params.get("roleIds");
            List<Long> roleIds = roleIdNums.stream().map(Number::longValue).toList();
            adminUserService.assignRoles(user.getId(), roleIds);
        }
        return R.ok();
    }

    @Operation(summary = "更新管理员")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        AdminUser user = parseUser(params);
        user.setId(id);
        adminUserService.updateUser(user);
        if (params.containsKey("roleIds")) {
            @SuppressWarnings("unchecked")
            List<Number> roleIdNums = (List<Number>) params.get("roleIds");
            List<Long> roleIds = roleIdNums.stream().map(Number::longValue).toList();
            adminUserService.assignRoles(id, roleIds);
        }
        return R.ok();
    }

    private AdminUser parseUser(Map<String, Object> params) {
        AdminUser user = new AdminUser();
        if (params.containsKey("username")) user.setUsername((String) params.get("username"));
        if (params.containsKey("password")) user.setPassword((String) params.get("password"));
        if (params.containsKey("nickname")) user.setNickname((String) params.get("nickname"));
        if (params.containsKey("phone")) user.setPhone((String) params.get("phone"));
        if (params.containsKey("email")) user.setEmail((String) params.get("email"));
        if (params.containsKey("deptId")) {
            Object deptId = params.get("deptId");
            if (deptId instanceof Number) user.setDeptId(((Number) deptId).longValue());
        }
        if (params.containsKey("status")) {
            Object status = params.get("status");
            if (status instanceof Number) user.setStatus(((Number) status).intValue());
        }
        return user;
    }

    @Operation(summary = "删除管理员")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        adminUserService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "分配角色")
    @PutMapping("/{id}/roles")
    public R<?> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        adminUserService.assignRoles(id, params.get("roleIds"));
        return R.ok();
    }
}