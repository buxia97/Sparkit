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

    // ============ 滑块验证码 ============

    private static final ConcurrentHashMap<String, SliderEntry> sliderCache = new ConcurrentHashMap<>();

    /**
     * 生成滑块验证码
     * @return {key, bgImage, sliderImage, y}
     */
    public static java.util.Map<String, Object> generateSlider() {
        String key = UUID.randomUUID().toString().replace("-", "");
        int canvasW = 300, canvasH = 160;
        int sliderW = 50, sliderH = 50;

        // 随机滑块位置
        int sliderX = sliderW + RANDOM.nextInt(canvasW - sliderW * 2);
        int sliderY = (canvasH - sliderH) / 2;

        BufferedImage bgImg = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bgImg.createGraphics();
        // 背景
        g.setColor(new Color(220, 225, 235));
        g.fillRect(0, 0, canvasW, canvasH);
        // 噪点
        for (int i = 0; i < 200; i++) {
            g.setColor(new Color(180 + RANDOM.nextInt(60), 185 + RANDOM.nextInt(60), 195 + RANDOM.nextInt(50)));
            g.fillRect(RANDOM.nextInt(canvasW), RANDOM.nextInt(canvasH), 2, 2);
        }
        // 绘制缺口位置（浅色标记）
        g.setColor(new Color(255, 255, 255, 180));
        g.fillRect(sliderX, sliderY, sliderW, sliderH);
        g.setColor(new Color(180, 190, 200));
        g.drawRect(sliderX, sliderY, sliderW, sliderH);
        g.dispose();

        // 滑块图片
        BufferedImage sliderImg = new BufferedImage(sliderW, sliderH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = sliderImg.createGraphics();
        g2.setColor(new Color(100, 140, 200));
        g2.fillRect(0, 0, sliderW, sliderH);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString(">>", 8, 32);
        g2.dispose();

        sliderCache.put(key, new SliderEntry(sliderX, System.currentTimeMillis()));

        try {
            ByteArrayOutputStream bgOs = new ByteArrayOutputStream();
            ImageIO.write(bgImg, "png", bgOs);
            ByteArrayOutputStream slOs = new ByteArrayOutputStream();
            ImageIO.write(sliderImg, "png", slOs);

            return java.util.Map.of(
                    "key", key,
                    "bgImage", "data:image/png;base64," + Base64.getEncoder().encodeToString(bgOs.toByteArray()),
                    "sliderImage", "data:image/png;base64," + Base64.getEncoder().encodeToString(slOs.toByteArray()),
                    "y", sliderY
            );
        } catch (Exception e) {
            log.error("生成滑块验证码失败", e);
            return java.util.Map.of();
        }
    }

    /**
     * 验证滑块验证码
     * @param key 验证码 key
     * @param sliderX 用户拖动的 X 坐标
     * @return 是否验证通过
     */
    public static boolean verifySlider(String key, int sliderX) {
        SliderEntry entry = sliderCache.remove(key);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.createTime > EXPIRE_MS) return false;
        // 允许 5px 误差
        return Math.abs(sliderX - entry.targetX) <= 5;
    }

    private static class SliderEntry {
        int targetX;
        long createTime;
        SliderEntry(int targetX, long createTime) { this.targetX = targetX; this.createTime = createTime; }
    }

    // ============ 腾讯云验证码 ============

    /**
     * 腾讯云验证码校验（前端传入 ticket 和 randstr）
     * 需要先在腾讯云验证码控制台注册并获取 AppId 和 AppSecretKey
     */
    public static boolean verifyTencentCaptcha(String ticket, String randstr, String appId, String appSecretKey) {
        if (ticket == null || ticket.isBlank()) return false;
        try {
            String url = "https://ssl.captcha.qq.com/ticket/verify"
                    + "?aid=" + appId
                    + "&AppSecretKey=" + appSecretKey
                    + "&Ticket=" + ticket
                    + "&Randstr=" + (randstr != null ? randstr : "")
                    + "&UserIP=0.0.0.0";

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code == 200) {
                java.io.InputStream is = conn.getInputStream();
                String resp = new String(is.readAllBytes());
                is.close();
                // 腾讯云返回 JSON: {"response":"1","evil_level":"0","err_msg":"..."}
                // response=1 表示验证通过
                return resp.contains("\"response\":\"1\"");
            }
            return false;
        } catch (Exception e) {
            log.error("腾讯云验证码校验失败", e);
            return false;
        }
    }
}