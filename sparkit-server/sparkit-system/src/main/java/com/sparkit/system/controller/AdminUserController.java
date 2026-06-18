package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.AdminUser;
import com.sparkit.system.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        return R.ok(adminUserService.login(params.get("username"), params.get("password")));
    }

    @GetMapping
    public R<PageResult<AdminUser>> list(PageQuery query) {
        return R.ok(adminUserService.page(query));
    }

    @GetMapping("/{id}")
    public R<AdminUser> getById(@PathVariable Long id) {
        return R.ok(adminUserService.getById(id));
    }

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

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        adminUserService.removeById(id);
        return R.ok();
    }

    @PutMapping("/{id}/roles")
    public R<?> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        adminUserService.assignRoles(id, params.get("roleIds"));
        return R.ok();
    }
}