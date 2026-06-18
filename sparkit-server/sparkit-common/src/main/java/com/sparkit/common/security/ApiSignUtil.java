package com.sparkit.common.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * API 签名工具
 * 支持 HMAC-SHA256 签名验证，防止请求被篡改
 */
@Slf4j
public class ApiSignUtil {

    private static final String DEFAULT_SECRET = "sparkit-api-sign-secret-key";

    /**
     * 生成签名
     * @param params 请求参数（自动按 key 排序）
     * @param secret 签名密钥
     * @param timestamp 时间戳
     * @param nonce 随机数
     */
    public static String generateSign(Map<String, Object> params, String secret, long timestamp, String nonce) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                sorted.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        sorted.put("timestamp", String.valueOf(timestamp));
        sorted.put("nonce", nonce);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.append("secret=").append(secret != null ? secret : DEFAULT_SECRET);

        return hmacSha256(sb.toString(), secret != null ? secret : DEFAULT_SECRET);
    }

    /**
     * 验证签名
     */
    public static boolean verifySign(Map<String, Object> params, String sign, String secret, long timestamp, String nonce) {
        // 检查时间戳（5分钟过期）
        if (Math.abs(System.currentTimeMillis() - timestamp) > 300_000) {
            log.warn("API签名时间戳过期: {}", timestamp);
            return false;
        }
        String expected = generateSign(params, secret, timestamp, nonce);
        return expected.equals(sign);
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("HMAC-SHA256 签名失败", e);
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}