package com.sparkit.notification.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.notification.model.entity.NotifyTemplate;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * 微信公众号模板消息通知策略（真实API调用）
 * 使用微信公众平台 API 发送模板消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatMpNotifyStrategy implements NotifyStrategy {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String TEMPLATE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConfigService configService;

    @Override
    public String getChannel() {
        return "wechat_mp";
    }

    @Override
    public String getChannelName() {
        return "微信公众号通知";
    }

    @Override
    public boolean send(NotifyTemplate template, String target, Map<String, String> params) {
        String appId = configService.getConfigValue("notification.wechat_mp.app_id");
        String appSecret = configService.getConfigValue("notification.wechat_mp.app_secret");

        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.warn("微信公众号未配置，跳过发送: openid={}", target);
            return false;
        }

        try {
            String accessToken = getAccessToken(appId, appSecret);
            if (accessToken == null) {
                log.error("获取微信公众号 access_token 失败: openid={}", target);
                return false;
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("touser", target);
            body.put("template_id", template.getTemplateCode());

            // 构建模板数据
            Map<String, Object> data = buildTemplateData(params, template.getContent());
            body.put("data", data);

            String jsonBody = MAPPER.writeValueAsString(body);
            String url = String.format(TEMPLATE_SEND_URL, accessToken);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("微信公众号模板消息发送完成: openid={}, resp={}", target, response.body());
            return response.body().contains("\"errcode\":0");
        } catch (Exception e) {
            log.error("微信公众号模板消息发送失败: openid={}, error={}", target, e.getMessage());
            return false;
        }
    }

    private String getAccessToken(String appId, String appSecret) {
        try {
            String url = String.format(TOKEN_URL, appId, appSecret);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);

            if (result.containsKey("access_token")) {
                return (String) result.get("access_token");
            }
            log.error("获取 access_token 失败: {}", response.body());
            return null;
        } catch (Exception e) {
            log.error("获取 access_token 异常", e);
            return null;
        }
    }

    private Map<String, Object> buildTemplateData(Map<String, String> params, String content) {
        Map<String, Object> data = new LinkedHashMap<>();

        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                data.put(entry.getKey(), Map.of("value", entry.getValue(), "color", "#173177"));
            }
        }

        return data;
    }

    private String replaceVariables(String template, Map<String, String> params) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}