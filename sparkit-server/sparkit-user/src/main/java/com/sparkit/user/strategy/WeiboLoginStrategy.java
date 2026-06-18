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
 * 微博 OAuth2.0 登录
 */
@Slf4j
@Component("weiboLoginStrategy")
@RequiredArgsConstructor
public class WeiboLoginStrategy implements SocialLoginStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final ConfigService configService;

    private String getAppKey() { return configService.getConfigValue("social.weibo.app_key"); }
    private String getAppSecret() { return configService.getConfigValue("social.weibo.app_secret"); }

    @Override
    public String getPlatform() { return "weibo"; }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        String appKey = getAppKey();
        if (appKey == null || appKey.isBlank()) throw new RuntimeException("微博登录未配置");
        return String.format(
                "https://api.weibo.com/oauth2/authorize" +
                "?client_id=%s&response_type=code&redirect_uri=%s&state=%s&scope=all",
                appKey, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8), state);
    }

    @Override
    public Map<String, Object> getAccessToken(String code, String redirectUri) {
        try {
            String appKey = getAppKey();
            String appSecret = getAppSecret();
            String url = String.format(
                    "https://api.weibo.com/oauth2/access_token" +
                    "?client_id=%s&client_secret=%s&grant_type=authorization_code&code=%s&redirect_uri=%s",
                    appKey, appSecret, code, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("error")) {
                log.error("微博获取 access_token 失败: {}", result);
                return Map.of("error", result.getOrDefault("error_description", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("微博获取 access_token 异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String uid) {
        try {
            String url = String.format(
                    "https://api.weibo.com/2/users/show.json?access_token=%s&uid=%s",
                    accessToken, uid);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("error")) {
                log.error("微博获取用户信息失败: {}", result);
                return Map.of("error", result.getOrDefault("error", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("微博获取用户信息异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        log.warn("微博 refresh_token 需要特殊流程，建议引导用户重新授权");
        return Map.of("error", "微博刷新token流程复杂，请重新授权");
    }
}