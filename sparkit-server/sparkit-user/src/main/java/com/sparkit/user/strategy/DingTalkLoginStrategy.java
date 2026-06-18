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
 * 钉钉 OAuth2.0 登录
 * 支持：扫码登录、企业内部应用免登
 */
@Slf4j
@Component("dingtalkLoginStrategy")
@RequiredArgsConstructor
public class DingTalkLoginStrategy implements SocialLoginStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final ConfigService configService;

    private String getAppKey() { return configService.getConfigValue("social.dingtalk.app_key"); }
    private String getAppSecret() { return configService.getConfigValue("social.dingtalk.app_secret"); }

    @Override
    public String getPlatform() { return "dingtalk"; }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        String appKey = getAppKey();
        if (appKey == null || appKey.isBlank()) throw new RuntimeException("钉钉登录未配置");
        return String.format(
                "https://login.dingtalk.com/oauth2/auth" +
                "?response_type=code&client_id=%s&redirect_uri=%s&scope=openid&state=%s&prompt=consent",
                appKey, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8), state);
    }

    @Override
    public Map<String, Object> getAccessToken(String code, String redirectUri) {
        try {
            String appKey = getAppKey();
            String appSecret = getAppSecret();

            Map<String, String> body = Map.of(
                    "clientId", appKey,
                    "clientSecret", appSecret,
                    "code", code,
                    "grantType", "authorization_code"
            );
            String jsonBody = MAPPER.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.dingtalk.com/v1.0/oauth2/userAccessToken"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("code") && !"0".equals(String.valueOf(result.get("code")))) {
                log.error("钉钉获取 access_token 失败: {}", result);
                return Map.of("error", result.getOrDefault("message", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("钉钉获取 access_token 异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String ignored) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.dingtalk.com/v1.0/contact/users/me"))
                    .header("x-acs-dingtalk-access-token", accessToken)
                    .GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            return result;
        } catch (Exception e) {
            log.error("钉钉获取用户信息异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        try {
            String appKey = getAppKey();
            String appSecret = getAppSecret();

            Map<String, String> body = Map.of(
                    "clientId", appKey,
                    "clientSecret", appSecret,
                    "refreshToken", refreshToken,
                    "grantType", "refresh_token"
            );
            String jsonBody = MAPPER.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.dingtalk.com/v1.0/oauth2/userAccessToken"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            return result;
        } catch (Exception e) {
            log.error("钉钉刷新 token 异常", e);
            return Map.of("error", e.getMessage());
        }
    }
}