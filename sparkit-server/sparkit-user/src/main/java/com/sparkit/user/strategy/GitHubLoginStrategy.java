package com.sparkit.user.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * GitHub OAuth2.0 登录
 */
@Slf4j
@Component("githubLoginStrategy")
@RequiredArgsConstructor
public class GitHubLoginStrategy implements SocialLoginStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final ConfigService configService;

    private String getClientId() { return configService.getConfigValue("social.github.client_id"); }
    private String getClientSecret() { return configService.getConfigValue("social.github.client_secret"); }

    @Override
    public String getPlatform() { return "github"; }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        String clientId = getClientId();
        if (clientId == null || clientId.isBlank()) throw new RuntimeException("GitHub登录未配置");
        return String.format(
                "https://github.com/login/oauth/authorize" +
                "?client_id=%s&redirect_uri=%s&state=%s&scope=user:email",
                clientId, redirectUri, state);
    }

    @Override
    public Map<String, Object> getAccessToken(String code, String redirectUri) {
        try {
            String clientId = getClientId();
            String clientSecret = getClientSecret();

            String jsonBody = MAPPER.writeValueAsString(Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "code", code,
                    "redirect_uri", redirectUri
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://github.com/login/oauth/access_token"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("error")) {
                log.error("GitHub获取 access_token 失败: {}", result);
                return Map.of("error", result.getOrDefault("error_description", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("GitHub获取 access_token 异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String ignored) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/user"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("message")) {
                log.error("GitHub获取用户信息失败: {}", result);
                return Map.of("error", result.get("message"));
            }
            return result;
        } catch (Exception e) {
            log.error("GitHub获取用户信息异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        log.warn("GitHub token 不过期，无需刷新");
        return Map.of("error", "GitHub token 不会过期");
    }
}