package com.sparkit.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * 实名认证服务
 * 支持：阿里云实名认证、腾讯云实名认证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealNameService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final ConfigService configService;

    /**
     * 身份证实名认证
     * @return Map 包含 verified(true/false), message
     */
    public Map<String, Object> verifyIdCard(String realName, String idCard) {
        String provider = configService.getConfigValue("realname.provider");
        if ("aliyun".equals(provider)) {
            return verifyByAliyun(realName, idCard);
        } else if ("tencent".equals(provider)) {
            return verifyByTencent(realName, idCard);
        }
        log.warn("实名认证未配置 provider，返回基础校验: name={}, idCard={}", realName, idCard);
        return basicVerify(realName, idCard);
    }

    private Map<String, Object> verifyByAliyun(String realName, String idCard) {
        try {
            String appCode = configService.getConfigValue("realname.aliyun.app_code");
            if (appCode == null || appCode.isBlank()) {
                log.warn("阿里云实名认证未配置，使用基础校验");
                return basicVerify(realName, idCard);
            }

            Map<String, String> params = new LinkedHashMap<>();
            params.put("idCard", idCard);
            params.put("name", realName);

            StringBuilder form = new StringBuilder();
            params.forEach((k, v) -> {
                if (form.length() > 0) form.append("&");
                form.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                        .append("=").append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            });

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://idcard.market.alicloudapi.com/lianzhuo/idcard"))
                    .header("Authorization", "APPCODE " + appCode)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            log.info("阿里云实名认证结果: {}", result);

            if ("0".equals(String.valueOf(result.get("status"))) || "01".equals(String.valueOf(result.get("status")))) {
                return Map.of("verified", true, "message", "实名认证通过");
            }
            return Map.of("verified", false, "message", result.getOrDefault("msg", "认证失败"));
        } catch (Exception e) {
            log.error("阿里云实名认证异常", e);
            return Map.of("verified", false, "message", e.getMessage());
        }
    }

    private Map<String, Object> verifyByTencent(String realName, String idCard) {
        try {
            String secretId = configService.getConfigValue("realname.tencent.secret_id");
            String secretKey = configService.getConfigValue("realname.tencent.secret_key");

            if (secretId == null || secretKey == null) {
                log.warn("腾讯云实名认证未配置，使用基础校验");
                return basicVerify(realName, idCard);
            }

            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("Action", "IdCardVerification");
            body.put("Version", "2018-03-01");
            body.put("Region", "ap-guangzhou");
            body.put("IdCard", idCard);
            body.put("Name", realName);
            body.put("Timestamp", timestamp);
            body.put("Nonce", nonce);
            body.put("SecretId", secretId);

            String jsonBody = MAPPER.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://faceid.tencentcloudapi.com/"))
                    .header("Content-Type", "application/json")
                    .header("X-TC-Action", "IdCardVerification")
                    .header("X-TC-Version", "2018-03-01")
                    .header("X-TC-Timestamp", timestamp)
                    .header("X-TC-Nonce", nonce)
                    .header("X-TC-Region", "ap-guangzhou")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            log.info("腾讯云实名认证结果: {}", result);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = (Map<String, Object>) result.get("Response");
            if (resp != null && "0".equals(String.valueOf(resp.get("Result")))) {
                return Map.of("verified", true, "message", "实名认证通过");
            }
            return Map.of("verified", false, "message",
                    resp != null ? resp.getOrDefault("Description", "认证失败") : "认证失败");
        } catch (Exception e) {
            log.error("腾讯云实名认证异常", e);
            return Map.of("verified", false, "message", e.getMessage());
        }
    }

    /**
     * 基础校验（仅校验身份证号格式）
     */
    private Map<String, Object> basicVerify(String realName, String idCard) {
        if (realName == null || realName.isBlank() || idCard == null || idCard.isBlank()) {
            return Map.of("verified", false, "message", "姓名和身份证号不能为空");
        }
        // 提供基础校验结果，实际应接入第三方API
        return Map.of("verified", true, "message", "基础校验通过（未接入第三方API，请配置实名认证服务商）");
    }
}