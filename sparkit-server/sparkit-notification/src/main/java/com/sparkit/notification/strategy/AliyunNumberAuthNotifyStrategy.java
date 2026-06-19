package com.sparkit.notification.strategy;

import com.sparkit.notification.model.entity.NotifyTemplate;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * 阿里云号码认证策略（一键登录/本机号码校验）
 * 与短信服务的区别：号码认证使用阿里云固定签名，无需用户自定义签名
 * 使用阿里云号码认证服务 dypnsapi（Cloud Phone Number Protection）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunNumberAuthNotifyStrategy implements NotifyStrategy {

    private static final String ENDPOINT = "dypnsapi.aliyuncs.com";
    private static final String API_VERSION = "2017-05-25";
    private static final String SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String SIGNATURE_VERSION = "1.0";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConfigService configService;

    @Override
    public String getChannel() {
        return "number_auth";
    }

    @Override
    public String getChannelName() {
        return "阿里云号码认证";
    }

    /**
     * 发送号码认证请求（一键登录获取手机号/本机号码校验）
     * 注意：号码认证使用阿里云固定签名，不支持自定义签名
     *
     * @param template 通知模板
     * @param target   目标（accessToken，即客户端SDK获取的token）
     * @param params   参数（action: GetMobile 或 VerifyMobile）
     */
    @Override
    public boolean send(NotifyTemplate template, String target, Map<String, String> params) {
        String accessKeyId = configService.getConfigValue("notification.number_auth.aliyun.access_key_id");
        String accessKeySecret = configService.getConfigValue("notification.number_auth.aliyun.access_key_secret");

        if (accessKeyId == null || accessKeyId.isBlank() || accessKeySecret == null || accessKeySecret.isBlank()) {
            log.warn("阿里云号码认证未配置 AccessKey，跳过: token={}", target);
            return false;
        }

        String action = params != null ? params.getOrDefault("action", "GetMobile") : "GetMobile";

        try {
            Map<String, String> queryParams = new TreeMap<>();
            queryParams.put("AccessKeyId", accessKeyId);
            queryParams.put("Action", action);
            queryParams.put("Format", "JSON");
            queryParams.put("AccessToken", target);
            queryParams.put("RegionId", "cn-hangzhou");
            queryParams.put("SignatureMethod", SIGNATURE_METHOD);
            queryParams.put("SignatureNonce", UUID.randomUUID().toString());
            queryParams.put("SignatureVersion", SIGNATURE_VERSION);
            queryParams.put("Timestamp", formatTimestamp(new Date()));
            queryParams.put("Version", API_VERSION);

            String signature = sign(buildQueryString(queryParams), accessKeySecret + "&");
            queryParams.put("Signature", signature);

            String queryString = buildQueryString(queryParams);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + ENDPOINT + "/?" + queryString))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("阿里云号码认证完成: action={}, resp={}", action, response.body());
            return response.body().contains("\"Code\":\"OK\"");
        } catch (Exception e) {
            log.error("阿里云号码认证失败: action={}, error={}", action, e.getMessage());
            return false;
        }
    }

    private String formatTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
        }
        return sb.toString();
    }

    private String sign(String queryString, String keySecret) throws NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException {
        String stringToSign = "POST&" + urlEncode("/") + "&" + urlEncode(queryString);
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(keySpec);
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}