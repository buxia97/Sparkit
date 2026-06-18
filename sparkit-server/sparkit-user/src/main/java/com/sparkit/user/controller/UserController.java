package com.sparkit.user.controller;

import com.sparkit.common.model.R;
import com.sparkit.user.model.entity.UserSocial;
import com.sparkit.user.service.UserService;
import com.sparkit.user.service.UserSocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户接口（C端）
 */
@RestController
@RequestMapping("/api/v1/public/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSocialService userSocialService;

    /** 发送验证码 */
    @PostMapping("/verify-code")
    public R<?> sendVerifyCode(@RequestBody Map<String, String> params) {
        userService.sendVerifyCode(params.get("target"), params.get("type"));
        return R.ok();
    }

    /** 手机号注册 */
    @PostMapping("/register/phone")
    public R<Map<String, Object>> registerByPhone(@RequestBody Map<String, String> params) {
        return R.ok(userService.registerByPhone(
                params.get("phone"), params.get("password"), params.get("code")));
    }

    /** 邮箱注册 */
    @PostMapping("/register/email")
    public R<Map<String, Object>> registerByEmail(@RequestBody Map<String, String> params) {
        return R.ok(userService.registerByEmail(
                params.get("email"), params.get("password"), params.get("code")));
    }

    /** 手机号验证码登录 */
    @PostMapping("/login/phone")
    public R<Map<String, Object>> loginByPhone(@RequestBody Map<String, String> params) {
        return R.ok(userService.loginByPhone(params.get("phone"), params.get("code")));
    }

    /** 账号密码登录 */
    @PostMapping("/login/password")
    public R<Map<String, Object>> loginByPassword(@RequestBody Map<String, String> params) {
        return R.ok(userService.loginByPassword(params.get("account"), params.get("password")));
    }

    /** 社交登录回调 */
    @PostMapping("/login/social")
    public R<Map<String, Object>> loginBySocial(@RequestBody Map<String, String> params) {
        return R.ok(userService.loginBySocial(
                params.get("platform"),
                params.get("openid"),
                params.get("unionid"),
                params.get("nickname"),
                params.get("avatar")));
    }

    /** 绑定社交账号 */
    @PostMapping("/social/bind")
    public R<?> bindSocial(@RequestBody Map<String, String> params) {
        Long userId = Long.valueOf(params.get("userId"));
        userSocialService.bind(userId,
                params.get("platform"),
                params.get("openid"),
                params.get("unionid"),
                params.get("nickname"),
                params.get("avatar"),
                params.get("accessToken"),
                params.get("refreshToken"));
        return R.ok();
    }
}