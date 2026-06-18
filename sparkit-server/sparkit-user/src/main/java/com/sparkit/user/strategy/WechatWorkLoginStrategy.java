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
 * 企业微信 OAuth2.0 登录
 */
@Slf4j
@Component("wechatWorkLoginStrategy")
@RequiredArgsConstructor
public class WechatWorkLoginStrategy implements SocialLoginStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final ConfigService configService;

    private String getCorpId() { return configService.getConfigValue("social.wechat_work.corp_id"); }
    private String getCorpSecret() { return configService.getConfigValue("social.wechat_work.corp_secret"); }
    private String getAgentId() { return configService.getConfigValue("social.wechat_work.agent_id"); }

    @Override
    public String getPlatform() { return "wechat_work"; }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        String corpId = getCorpId();
        if (corpId == null || corpId.isBlank()) throw new RuntimeException("企业微信登录未配置");
        return String.format(
                "https://open.weixin.qq.com/connect/oauth2/authorize" +
                "?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_privateinfo&state=%s#wechat_redirect",
                corpId, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8), state);
    }

    @Override
    public Map<String, Object> getAccessToken(String code, String redirectUri) {
        try {
            String corpId = getCorpId();
            String corpSecret = getCorpSecret();
            String url = String.format(
                    "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s",
                    corpId, corpSecret);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("errcode") && (Integer) result.get("errcode") != 0) {
                log.error("企业微信获取 access_token 失败: {}", result);
                return Map.of("error", result.getOrDefault("errmsg", "unknown error"));
            }
            // 企业微信需要额外步骤：用 code 获取 userid
            String accessToken = (String) result.get("access_token");
            String userInfoUrl = String.format(
                    "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=%s&code=%s",
                    accessToken, code);
            request = HttpRequest.newBuilder()
                    .uri(URI.create(userInfoUrl)).GET().timeout(Duration.ofSeconds(10)).build();
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> userResult = MAPPER.readValue(response.body(), Map.class);
            userResult.put("access_token", accessToken);
            return userResult;
        } catch (Exception e) {
            log.error("企业微信获取用户信息异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String userId) {
        try {
            String url = String.format(
                    "https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=%s&userid=%s",
                    accessToken, userId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if (result.containsKey("errcode") && (Integer) result.get("errcode") != 0) {
                log.error("企业微信获取用户详情失败: {}", result);
                return Map.of("error", result.getOrDefault("errmsg", "unknown error"));
            }
            return result;
        } catch (Exception e) {
            log.error("企业微信获取用户详情异常", e);
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        log.warn("企业微信 access_token 通过 corpId + corpSecret 重新获取");
        return Map.of("error", "请重新获取企业微信 access_token");
    }
}