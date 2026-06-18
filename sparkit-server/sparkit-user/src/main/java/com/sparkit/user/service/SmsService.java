package com.sparkit.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
 * 短信服务
 * 支持：阿里云短信、飞鸽云短信
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final ConfigService configService;

    /**
     * 发送短信验证码
     */
    public boolean sendVerifyCode(String phone, String code) {
        String provider = configService.getConfigValue("sms.provider");
        if ("aliyun".equals(provider)) {
            return sendByAliyun(phone, code);
        } else if ("feige".equals(provider)) {
            return sendByFeige(phone, code);
        } else {
            log.warn("短信服务未配置 provider，仅打印验证码: phone={}, code={}", phone, code);
            return true; // 开发环境默认返回成功
        }
    }

    /**
     * 阿里云短信发送
     */
    private boolean sendByAliyun(String phone, String code) {
        try {
            String accessKeyId = configService.getConfigValue("sms.aliyun.access_key_id");
            String accessKeySecret = configService.getConfigValue("sms.aliyun.access_key_secret");
            String signName = configService.getConfigValue("sms.aliyun.sign_name");
            String templateCode = configService.getConfigValue("sms.aliyun.template_code");

            if (accessKeyId == null || accessKeySecret == null) {
                log.warn("阿里云短信未配置，仅打印验证码: phone={}, code={}", phone, code);
                return true;
            }

            Map<String, String> params = new LinkedHashMap<>();
            params.put("PhoneNumbers", phone);
            params.put("SignName", signName);
            params.put("TemplateCode", templateCode);
            params.put("TemplateParam", "{\"code\":\"" + code + "\"}");
            params.put("AccessKeyId", accessKeyId);
            params.put("Action", "SendSms");
            params.put("Version", "2017-05-25");
            params.put("Timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .format(new Date()) + "Z");
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureVersion", "1.0");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("Format", "JSON");

            String signature = computeAliyunSignature(params, accessKeySecret);
            params.put("Signature", signature);

            StringBuilder urlBuilder = new StringBuilder("https://dysmsapi.aliyuncs.com/?");
            params.forEach((k, v) -> urlBuilder.append(k).append("=")
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8)).append("&"));
            String url = urlBuilder.substring(0, urlBuilder.length() - 1);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if ("OK".equals(result.get("Code"))) {
                log.info("阿里云短信发送成功: phone={}", phone);
                return true;
            }
            log.error("阿里云短信发送失败: {}", result);
            return false;
        } catch (Exception e) {
            log.error("阿里云短信发送异常", e);
            return false;
        }
    }

    /**
     * 飞鸽云短信发送
     */
    private boolean sendByFeige(String phone, String code) {
        try {
            String accessKeyId = configService.getConfigValue("sms.feige.access_key_id");
            String accessKeySecret = configService.getConfigValue("sms.feige.access_key_secret");
            String signId = configService.getConfigValue("sms.feige.sign_id");
            String templateId = configService.getConfigValue("sms.feige.template_id");

            if (accessKeyId == null || accessKeySecret == null) {
                log.warn("飞鸽云短信未配置，仅打印验证码: phone={}, code={}", phone, code);
                return true;
            }

            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String sign = md5(accessKeyId + accessKeySecret + timestamp);

            Map<String, String> body = new LinkedHashMap<>();
            body.put("accesskey", accessKeyId);
            body.put("secret", accessKeySecret);
            body.put("sign", sign);
            body.put("templateId", templateId);
            body.put("mobile", phone);
            body.put("content", code);
            body.put("signId", signId);

            StringBuilder form = new StringBuilder();
            body.forEach((k, v) -> {
                if (form.length() > 0) form.append("&");
                form.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                        .append("=").append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            });

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.feige.ee/v1/sms/send"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
            if ("0".equals(String.valueOf(result.get("code")))) {
                log.info("飞鸽云短信发送成功: phone={}", phone);
                return true;
            }
            log.error("飞鸽云短信发送失败: {}", result);
            return false;
        } catch (Exception e) {
            log.error("飞鸽云短信发送异常", e);
            return false;
        }
    }

    /**
     * 计算阿里云 API 签名
     */
    private String computeAliyunSignature(Map<String, String> params, String secret) throws Exception {
        String[] sortedKeys = params.keySet().toArray(new String[0]);
        Arrays.sort(sortedKeys);
        StringBuilder canonical = new StringBuilder();
        for (String key : sortedKeys) {
            canonical.append("&").append(percentEncode(key))
                    .append("=").append(percentEncode(params.get(key)));
        }
        String stringToSign = "GET&" + percentEncode("/") + "&" +
                percentEncode(canonical.substring(1));

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((secret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    private String percentEncode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}