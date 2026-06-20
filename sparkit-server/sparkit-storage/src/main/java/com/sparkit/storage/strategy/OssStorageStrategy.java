package com.sparkit.storage.strategy;

import com.sparkit.storage.model.entity.StorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * 阿里云 OSS 存储策略 - 真实 HTTP API 调用
 * 使用 OSS REST API 签名认证上传/下载/删除
 */
@Slf4j
@Component
public class OssStorageStrategy implements StorageStrategy {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();

    @Override public String getType() { return "aliyun-oss"; }

    @Override
    public String upload(MultipartFile file, StorageConfig config) throws Exception {
        return upload(file.getBytes(), file.getOriginalFilename(), config);
    }

    @Override
    public String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception {
        String path = buildPath(config, fileName);
        String host = config.getBucket() + "." + config.getEndpoint();
        String url = "https://" + host + "/" + path;
        String contentType = getContentType(fileName);
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.now(ZoneId.of("GMT")).withNano(0));

        String stringToSign = "PUT\n\n" + contentType + "\n" + date + "\n/" + config.getBucket() + "/" + path;
        String signature = hmacSha1(stringToSign, config.getAccessKeySecret());
        String auth = "OSS " + config.getAccessKeyId() + ":" + signature;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .header("Content-Type", contentType)
                .header("Date", date)
                .header("Authorization", auth)
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        log.info("OSS上传: path={} status={}", path, resp.statusCode());
        if (resp.statusCode() != 200) throw new RuntimeException("OSS上传失败: " + resp.body());
        return path;
    }

    @Override
    public String getFileUrl(String path, StorageConfig config) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + path;
        }
        return "https://" + config.getBucket() + "." + config.getEndpoint() + "/" + path;
    }

    @Override public String getPreviewUrl(String path, StorageConfig config) { return getFileUrl(path, config); }

    @Override
    public boolean delete(String path, StorageConfig config) {
        try {
            String host = config.getBucket() + "." + config.getEndpoint();
            String url = "https://" + host + "/" + path;
            String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                    ZonedDateTime.now(ZoneId.of("GMT")).withNano(0));
            String stringToSign = "DELETE\n\n\n" + date + "\n/" + config.getBucket() + "/" + path;
            String signature = hmacSha1(stringToSign, config.getAccessKeySecret());
            String auth = "OSS " + config.getAccessKeyId() + ":" + signature;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .header("Date", date)
                    .header("Authorization", auth)
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("OSS删除: path={} status={}", path, resp.statusCode());
            return resp.statusCode() == 204;
        } catch (Exception e) {
            log.error("OSS删除失败: path={}", path, e);
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

    private String hmacSha1(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String getContentType(String fileName) {
        String ext = fileName.toLowerCase();
        if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
        if (ext.endsWith(".png")) return "image/png";
        if (ext.endsWith(".gif")) return "image/gif";
        if (ext.endsWith(".pdf")) return "application/pdf";
        if (ext.endsWith(".json")) return "application/json";
        if (ext.endsWith(".xml")) return "application/xml";
        return "application/octet-stream";
    }
}