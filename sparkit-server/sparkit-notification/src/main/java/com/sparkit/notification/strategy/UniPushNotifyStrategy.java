package com.sparkit.notification.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.notification.model.entity.NotifyTemplate;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * UniPush App 推送策略（DCloud UniPush 真实API调用）
 * 使用 DCloud UniPush REST API v2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniPushNotifyStrategy implements NotifyStrategy {

    private static final String UNIPUSH_API = "https://fcapi.dcloud.net.cn/api/v2/push/single";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConfigService configService;

    @Override
    public String getChannel() {
        return "unipush";
    }

    @Override
    public String getChannelName() {
        return "UniPush推送";
    }

    @Override
    public boolean send(NotifyTemplate template, String target, Map<String, String> params) {
        String appId = configService.getConfigValue("notification.unipush.app_id");
        String appKey = configService.getConfigValue("notification.unipush.app_key");
        String masterSecret = configService.getConfigValue("notification.unipush.master_secret");

        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank() || masterSecret == null || masterSecret.isBlank()) {
            log.warn("UniPush 未配置，跳过推送: cid={}", target);
            return false;
        }

        String content = replaceVariables(template.getContent(), params);
        String title = replaceVariables(template.getTitle(), params);

        try {
            long timestamp = System.currentTimeMillis();
            String payload = buildPayload(appId, target, title, content);
            String sign = hmacSha256(appKey + timestamp + masterSecret, appKey);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", sign);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(UNIPUSH_API))
                    .header("Content-Type", "application/json")
                    .header("Authorization", sign)
                    .header("UniPush-AppId", appId)
                    .header("UniPush-Timestamp", String.valueOf(timestamp))
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("UniPush 推送完成: cid={}, resp={}", target, response.body());
            return response.body().contains("\"code\":0");
        } catch (Exception e) {
            log.error("UniPush 推送失败: cid={}, error={}", target, e.getMessage());
            return false;
        }
    }

    private String buildPayload(String appId, String cid, String title, String content) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("appid", appId);
            payload.put("cid", cid);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("payload", "{}");

            Map<String, Object> options = new LinkedHashMap<>();
            Map<String, Object> android = new LinkedHashMap<>();
            android.put("sound", "default");
            options.put("HW", android);
            options.put("VV", android);
            payload.put("options", options);

            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("构建UniPush payload失败", e);
            return "{}";
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] signData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            log.error("HMAC-SHA256签名失败", e);
            return "";
        }
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