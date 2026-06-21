package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.framework.service.LoginStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 登录策略管理
 */
@Tag(name = "登录策略管理", description = "单设备/多设备登录策略、账号锁定/解锁")
@RestController
@RequiredArgsConstructor
public class LoginStrategyController {

    private final LoginStrategyService loginStrategyService;

    /** 获取当前登录策略 */
    @Operation(summary = "获取登录策略")
    @GetMapping("/api/v1/admin/login-strategy")
    public R<Map<String, String>> getStrategy() {
        String strategy = loginStrategyService.getLoginStrategy("global");
        return R.ok(Map.of("strategy", strategy));
    }

    /** 设置登录策略：single=单设备，multi=多设备 */
    @Operation(summary = "设置登录策略")
    @PutMapping("/api/v1/admin/login-strategy")
    public R<?> setStrategy(@RequestBody Map<String, String> params) {
        String strategy = params.get("strategy");
        if (!"single".equals(strategy) && !"multi".equals(strategy)) {
            return R.fail("策略参数无效，仅支持 single 或 multi");
        }
        loginStrategyService.setLoginStrategy("global", strategy);
        return R.ok();
    }

    /** 解锁账号 */
    @Operation(summary = "解锁账号")
    @PostMapping("/api/v1/admin/login-strategy/unlock")
    public R<?> unlockAccount(@RequestParam String username) {
        loginStrategyService.unlockAccount(username);
        return R.ok();
    }

    /** 检查账号是否被锁定 */
    @Operation(summary = "检查账号锁定状态")
    @GetMapping("/api/v1/admin/login-strategy/is-locked")
    public R<Map<String, Boolean>> isLocked(@RequestParam String username) {
        boolean locked = loginStrategyService.isLocked(username);
        return R.ok(Map.of("locked", locked));
    }
}