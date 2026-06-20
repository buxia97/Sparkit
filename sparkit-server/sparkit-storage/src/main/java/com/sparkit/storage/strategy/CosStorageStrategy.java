package com.sparkit.storage.strategy;

import com.sparkit.storage.model.entity.StorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 腾讯云 COS 存储策略 - 真实 HTTP API 调用
 * 使用 COS REST API 签名认证上传/下载/删除
 */
@Slf4j
@Component
public class CosStorageStrategy implements StorageStrategy {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();

    @Override public String getType() { return "tencent-cos"; }

    @Override
    public String upload(MultipartFile file, StorageConfig config) throws Exception {
        return upload(file.getBytes(), file.getOriginalFilename(), config);
    }

    @Override
    public String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception {
        String path = buildPath(config, fileName);
        String host = config.getBucket() + ".cos." + config.getEndpoint() + ".myqcloud.com";
        String url = "https://" + host + "/" + path;
        String contentType = getContentType(fileName);

        long startTime = System.currentTimeMillis() / 1000 - 60;
        long endTime = startTime + 3600;
        String keyTime = startTime + ";" + endTime;

        String httpString = "put\n/" + path + "\n\nhost=" + host + "\n";
        String sha1Http = sha1(httpString);
        String stringToSign = "sha1\n" + keyTime + "\n" + sha1Http + "\n";
        String signKey = hmacSha1(keyTime, config.getAccessKeySecret());
        String signature = hmacSha1(stringToSign, signKey);

        String auth = "q-sign-algorithm=sha1&q-ak=" + config.getAccessKeyId()
                + "&q-sign-time=" + keyTime + "&q-key-time=" + keyTime
                + "&q-header-list=host&q-url-param-list=&q-signature=" + signature;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .header("Content-Type", contentType)
                .header("Host", host)
                .header("Authorization", auth)
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        log.info("COS上传: path={} status={}", path, resp.statusCode());
        if (resp.statusCode() != 200) throw new RuntimeException("COS上传失败: " + resp.body());
        return path;
    }

    @Override
    public String getFileUrl(String path, StorageConfig config) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + path;
        }
        return "https://" + config.getBucket() + ".cos." + config.getEndpoint() + ".myqcloud.com/" + path;
    }

    @Override public String getPreviewUrl(String path, StorageConfig config) { return getFileUrl(path, config); }

    @Override
    public boolean delete(String path, StorageConfig config) {
        try {
            String host = config.getBucket() + ".cos." + config.getEndpoint() + ".myqcloud.com";
            String url = "https://" + host + "/" + path;
            long startTime = System.currentTimeMillis() / 1000 - 60;
            long endTime = startTime + 3600;
            String keyTime = startTime + ";" + endTime;

            String httpString = "delete\n/" + path + "\n\nhost=" + host + "\n";
            String sha1Http = sha1(httpString);
            String stringToSign = "sha1\n" + keyTime + "\n" + sha1Http + "\n";
            String signKey = hmacSha1(keyTime, config.getAccessKeySecret());
            String signature = hmacSha1(stringToSign, signKey);
            String auth = "q-sign-algorithm=sha1&q-ak=" + config.getAccessKeyId()
                    + "&q-sign-time=" + keyTime + "&q-key-time=" + keyTime
                    + "&q-header-list=host&q-url-param-list=&q-signature=" + signature;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).DELETE()
                    .header("Host", host).header("Authorization", auth)
                    .timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("COS删除: path={} status={}", path, resp.statusCode());
            return resp.statusCode() == 204;
        } catch (Exception e) {
            log.error("COS删除失败: path={}", path, e);
            return false;
        }
    }

    @Override
    public InputStream download(String path, StorageConfig config) throws Exception {
        return new java.net.URL(getFileUrl(path, config)).openStream();
    }

    @Override
    public boolean exists(String path, StorageConfig config) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(getFileUrl(path, config)).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) { return false; }
    }

    private String buildPath(StorageConfig config, String fileName) {
        String base = config.getBasePath() != null ? config.getBasePath() : "";
        return base + "/" + fileName;
    }

    private String sha1(String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return bytesToHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String hmacSha1(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return bytesToHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String getContentType(String fileName) {
        String ext = fileName.toLowerCase();
        if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
        if (ext.endsWith(".png")) return "image/png";
        if (ext.endsWith(".gif")) return "image/gif";
        if (ext.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}