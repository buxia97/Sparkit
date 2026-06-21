package com.sparkit.user.controller;

import com.sparkit.common.model.R;
import com.sparkit.user.model.entity.UserSocial;
import com.sparkit.user.service.EmailService;
import com.sparkit.user.service.UserService;
import com.sparkit.user.service.UserSocialService;
import com.sparkit.user.strategy.WechatLoginStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户接口（C端）
 */
@Tag(name = "用户接口（C端）", description = "用户注册、登录、社交登录绑定")
@RestController
@RequestMapping("/api/v1/public/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSocialService userSocialService;
    private final WechatLoginStrategy wechatLoginStrategy;

    @Operation(summary = "发送验证码")
    @PostMapping("/verify-code")
    public R<?> sendVerifyCode(@RequestBody Map<String, String> params) {
        userService.sendVerifyCode(params.get("target"), params.get("type"));
        return R.ok();
    }

    @Operation(summary = "手机号注册")
    @PostMapping("/register/phone")
    public R<Map<String, Object>> registerByPhone(@RequestBody Map<String, String> params) {
        return R.ok(userService.registerByPhone(
                params.get("phone"), params.get("password"), params.get("code")));
    }

    @Operation(summary = "邮箱注册")
    @PostMapping("/register/email")
    public R<Map<String, Object>> registerByEmail(@RequestBody Map<String, String> params) {
        return R.ok(userService.registerByEmail(
                params.get("email"), params.get("password"), params.get("code")));
    }

    @Operation(summary = "手机验证码登录")
    @PostMapping("/login/phone")
    public R<Map<String, Object>> loginByPhone(@RequestBody Map<String, String> params) {
        return R.ok(userService.loginByPhone(params.get("phone"), params.get("code")));
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login/password")
    public R<Map<String, Object>> loginByPassword(@RequestBody Map<String, String> params) {
        return R.ok(userService.loginByPassword(params.get("account"), params.get("password")));
    }

    @Operation(summary = "获取社交登录授权 URL")
    @GetMapping("/social/authorize-url")
    public R<Map<String, String>> getSocialAuthorizeUrl(
            @RequestParam String platform,
            @RequestParam String redirectUri,
            @RequestParam(required = false) String state) {
        if (state == null) state = java.util.UUID.randomUUID().toString().replace("-", "");
        String url = userService.getSocialAuthorizeUrl(platform, redirectUri, state);
        return R.ok(Map.of("url", url, "state", state));
    }

    @Operation(summary = "OAuth 回调登录")
    @PostMapping("/login/social-oauth")
    public R<Map<String, Object>> loginBySocialOAuth(@RequestBody Map<String, String> params) {
        return R.ok(userService.loginBySocial(
                params.get("platform"),
                params.get("code"),
                params.get("redirectUri")));
    }

    @Operation(summary = "微信小程序登录")
    @PostMapping("/login/wechat-mini")
    public R<Map<String, Object>> loginByWechatMini(@RequestBody Map<String, String> params) {
        Map<String, Object> sessionResult = wechatLoginStrategy.miniProgramLogin(params.get("code"));
        if (sessionResult.containsKey("error")) {
            return R.fail(400, (String) sessionResult.get("error"));
        }
        String openid = (String) sessionResult.get("openid");
        String unionid = (String) sessionResult.get("unionid");
        return R.ok(userService.loginBySocial("wechat", openid, unionid, null, null));
    }

    @Operation(summary = "社交登录（旧版兼容）")
    @PostMapping("/login/social")
    public R<Map<String, Object>> loginBySocial(@RequestBody Map<String, String> params) {
        return R.ok(userService.loginBySocial(
                params.get("platform"),
                params.get("openid"),
                params.get("unionid"),
                params.get("nickname"),
                params.get("avatar")));
    }

    @Operation(summary = "绑定社交账号")
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