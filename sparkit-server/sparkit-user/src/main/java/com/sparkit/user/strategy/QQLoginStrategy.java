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
import java.util.Map;

/**
 * QQ 互联 OAuth2.0 登录
 */
@Slf4j
@Component("qqLoginStrategy")
@RequiredArgsConstructor
public class QQLoginStrategy implements SocialLoginStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final ConfigService configService;

    private String getAppId() { return configService.getConfigValue("social.qq.app_id"); }
    private String getAppKey() { return configService.getConfigValue("social.qq.app_key"); }

    @Override
    public String getPlatform() { return "qq"; }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        String appId = getAppId();
        if (appId == null || appId.isBlank()) throw new RuntimeException("QQ登录未配置");
        return String.format(
                "https://graph.qq.com/oauth2.0/authorize" +
                "?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=get_user_info",
                appId, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8), state);
    }

    @Override
    public Map<String, Object> getAccessToken(String code, String redirectUri) {
        try {
            String appId = getAppId();
            String appKey = getAppKey();
            String url = String.format(
                    "https://graph.qq.com/oauth2.0/token" +
                    "?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&fmt=json",
                    appId, appKey, code, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("error")) {
                log.error("QQ获取 access_token 失败: {}", result);
                return Map.of("error", result.getOrDefault("error_description", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("QQ获取 access_token 异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    /** QQ 需要先获取 openid */
    public Map<String, Object> getOpenId(String accessToken) {
        try {
            String url = "https://graph.qq.com/oauth2.0/me?access_token=" + accessToken + "&fmt=json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            return result;
        } catch (Exception e) {
            log.error("QQ获取 openid 异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String openid) {
        try {
            String appId = getAppId();
            String url = String.format(
                    "https://graph.qq.com/user/get_user_info" +
                    "?access_token=%s&oauth_consumer_key=%s&openid=%s",
                    accessToken, appId, openid);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("ret") && (Integer) result.get("ret") != 0) {
                log.error("QQ获取用户信息失败: {}", result);
                return Map.of("error", result.getOrDefault("msg", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("QQ获取用户信息异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        log.warn("QQ access_token 有效期较长，通常不需要刷新");
        return Map.of("error", "QQ不支持刷新token");
    }
}