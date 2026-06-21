package com.sparkit.user.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.user.model.entity.User;
import com.sparkit.user.model.entity.UserSocial;
import com.sparkit.user.service.UserService;
import com.sparkit.user.service.UserSocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * C端用户管理（管理端）
 */
@Tag(name = "C端用户管理", description = "C端用户管理、黑名单、实名认证、社交绑定")
@RestController("cUserAdminController")
@RequestMapping("/api/v1/admin/users/c")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final UserSocialService userSocialService;

    @Operation(summary = "C端用户列表")
    @GetMapping
    public R<PageResult<User>> list(PageQuery query,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String phone,
                                     @RequestParam(required = false) Integer status) {
        return R.ok(userService.adminPage(query, keyword, phone, status));
    }

    @Operation(summary = "获取C端用户详情")
    @GetMapping("/{id}")
    public R<User> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @Operation(summary = "更新C端用户信息")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateById(user);
        return R.ok();
    }

    @Operation(summary = "更新C端用户状态")
    @PutMapping("/{id}/status")
    public R<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        User user = userService.getById(id);
        if (user != null) {
            user.setStatus(params.get("status"));
            userService.updateById(user);
        }
        return R.ok();
    }

    @Operation(summary = "设置C端用户等级")
    @PutMapping("/{id}/level")
    public R<?> updateLevel(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        User user = userService.getById(id);
        if (user != null) {
            user.setLevel(params.get("level"));
            userService.updateById(user);
        }
        return R.ok();
    }

    @Operation(summary = "C端用户黑名单管理")
    @PutMapping("/{id}/blacklist")
    public R<?> toggleBlacklist(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        User user = userService.getById(id);
        if (user != null) {
            user.setIsBlacklisted(params.get("isBlacklisted"));
            userService.updateById(user);
        }
        return R.ok();
    }

    @Operation(summary = "实名认证审核")
    @PutMapping("/{id}/real-name")
    public R<?> auditRealName(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        User user = userService.getById(id);
        if (user != null) {
            user.setRealNameStatus(params.get("status"));
            userService.updateById(user);
        }
        return R.ok();
    }

    @Operation(summary = "获取C端用户社交绑定")
    @GetMapping("/{id}/social")
    public R<List<UserSocial>> socialList(@PathVariable Long id) {
        return R.ok(userSocialService.listByUserId(id));
    }

    @Operation(summary = "解除社交绑定")
    @DeleteMapping("/{id}/social/{platform}")
    public R<?> unbindSocial(@PathVariable Long id, @PathVariable String platform) {
        userSocialService.unbind(id, platform);
        return R.ok();
    }

    @Operation(summary = "删除C端用户")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }
}