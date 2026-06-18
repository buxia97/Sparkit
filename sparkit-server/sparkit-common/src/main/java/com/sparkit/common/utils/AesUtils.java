package com.sparkit.common.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加密工具类
 * 用于敏感字段（身份证、API Key 等）加密存储
 */
@Slf4j
public class AesUtils {

    private static final String DEFAULT_KEY = "Sparkit@2024!AES";
    private static final AES AES_INSTANCE = SecureUtil.aes(DEFAULT_KEY.getBytes(StandardCharsets.UTF_8));

    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            return Base64.getEncoder().encodeToString(AES_INSTANCE.encrypt(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("AES encrypt error", e);
            return plainText;
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(cipherText);
            return new String(AES_INSTANCE.decrypt(bytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES decrypt error", e);
            return cipherText;
        }
    }
}