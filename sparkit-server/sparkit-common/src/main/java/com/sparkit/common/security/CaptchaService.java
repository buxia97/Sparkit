package com.sparkit.common.security;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 图片验证码服务
 */
@Slf4j
public class CaptchaService {

    private static final ConcurrentHashMap<String, CaptchaEntry> cache = new ConcurrentHashMap<>();
    private static final long EXPIRE_MS = 300_000L; // 5分钟过期
    private static final Random RANDOM = new Random();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去掉易混淆字符

    /** 生成验证码，返回 {key, base64Image} */
    public static java.util.Map<String, String> generate() {
        String key = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode(4);
        String image = renderImage(code);

        cache.put(key, new CaptchaEntry(code, System.currentTimeMillis()));
        // 清理过期
        cache.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().createTime > EXPIRE_MS);

        return java.util.Map.of("key", key, "image", "data:image/png;base64," + image);
    }

    /** 验证验证码 */
    public static boolean verify(String key, String code) {
        CaptchaEntry entry = cache.remove(key);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.createTime > EXPIRE_MS) return false;
        return entry.code.equalsIgnoreCase(code != null ? code.trim() : "");
    }

    private static String randomCode(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private static String renderImage(String code) {
        int w = 120, h = 44;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // 背景
        g.setColor(new Color(240, 245, 250));
        g.fillRect(0, 0, w, h);

        // 干扰线
        g.setColor(new Color(200, 210, 220));
        for (int i = 0; i < 5; i++) {
            g.drawLine(RANDOM.nextInt(w), RANDOM.nextInt(h), RANDOM.nextInt(w), RANDOM.nextInt(h));
        }

        // 文字
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(20 + RANDOM.nextInt(100), 40 + RANDOM.nextInt(100), 60 + RANDOM.nextInt(100)));
            g.drawString(String.valueOf(code.charAt(i)), 15 + i * 24, 28 + RANDOM.nextInt(6));
        }

        g.dispose();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", os);
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (Exception e) {
            log.error("生成验证码失败", e);
            return "";
        }
    }

    private static class CaptchaEntry {
        String code;
        long createTime;
        CaptchaEntry(String code, long createTime) { this.code = code; this.createTime = createTime; }
    }
}