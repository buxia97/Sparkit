package com.sparkit.user.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信 OAuth2.0 登录策略
 * 支持：公众号网页授权、微信开放平台扫码登录、小程序登录
 */
@Slf4j
@Component("wechatLoginStrategy")
@RequiredArgsConstructor
public class WechatLoginStrategy implements SocialLoginStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConfigService configService;

    private String getAppId() { return configService.getConfigValue("social.wechat.app_id"); }
    private String getAppSecret() { return configService.getConfigValue("social.wechat.app_secret"); }
    private String getMiniAppId() { return configService.getConfigValue("social.wechat.mini_app_id"); }
    private String getMiniAppSecret() { return configService.getConfigValue("social.wechat.mini_app_secret"); }

    @Override
    public String getPlatform() { return "wechat"; }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        String appId = getAppId();
        if (appId == null || appId.isBlank()) throw new RuntimeException("微信登录未配置");
        return String.format(
                "https://open.weixin.qq.com/connect/oauth2/authorize" +
                "?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=%s#wechat_redirect",
                appId, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8), state);
    }

    @Override
    public Map<String, Object> getAccessToken(String code, String redirectUri) {
        try {
            String appId = getAppId();
            String appSecret = getAppSecret();
            String url = String.format(
                    "https://api.weixin.qq.com/sns/oauth2/access_token" +
                    "?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                    appId, appSecret, code);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("errcode")) {
                log.error("微信获取 access_token 失败: {}", result);
                return Map.of("error", result.getOrDefault("errmsg", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("微信获取 access_token 异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String openid) {
        try {
            String url = String.format(
                    "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN",
                    accessToken, openid);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("errcode")) {
                log.error("微信获取用户信息失败: {}", result);
                return Map.of("error", result.getOrDefault("errmsg", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("微信获取用户信息异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        try {
            String appId = getAppId();
            String url = String.format(
                    "https://api.weixin.qq.com/sns/oauth2/refresh_token" +
                    "?appid=%s&grant_type=refresh_token&refresh_token=%s",
                    appId, refreshToken);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            return result;
        } catch (Exception e) {
            log.error("微信刷新 token 异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 小程序登录（通过 code 换取 openid 和 session_key）
     */
    public Map<String, Object> miniProgramLogin(String code) {
        try {
            String appId = getMiniAppId();
            String appSecret = getMiniAppSecret();
            String url = String.format(
                    "https://api.weixin.qq.com/sns/jscode2session" +
                    "?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    appId, appSecret, code);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("errcode") && (Integer) result.get("errcode") != 0) {
                log.error("微信小程序登录失败: {}", result);
                return Map.of("error", result.getOrDefault("errmsg", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("微信小程序登录异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 微信公众号服务器验证（用于接入验证）
     */
    public boolean verifySignature(String signature, String timestamp, String nonce) {
        try {
            String token = configService.getConfigValue("social.wechat.mp_token");
            if (token == null) return false;
            String[] arr = {token, timestamp, nonce};
            java.util.Arrays.sort(arr);
            String str = String.join("", arr);
            String sha1 = java.security.MessageDigest.getInstance("SHA-1")
                    .digest(str.getBytes(StandardCharsets.UTF_8))
                    .toString();
            return signature.equals(sha1);
        } catch (Exception e) {
            return false;
        }
    }
}