package com.sparkit.storage.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * 七牛云 Kodo 存储策略 - 真实 HTTP API 调用
 * 使用七牛云 upload token 认证上传/删除
 */
@Slf4j
@Component
public class KodoStorageStrategy implements StorageStrategy {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String getType() { return "qiniu-kodo"; }

    @Override
    public String upload(MultipartFile file, StorageConfig config) throws Exception {
        return upload(file.getBytes(), file.getOriginalFilename(), config);
    }

    @Override
    public String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception {
        String path = buildPath(config, fileName);
        String uploadToken = generateUploadToken(config, path);
        String uploadUrl = "https://up-" + config.getEndpoint() + "/";

        // 七牛云使用 multipart/form-data 上传
        String boundary = "----FormBoundary" + System.currentTimeMillis();
        StringBuilder body = new StringBuilder();
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"token\"\r\n\r\n");
        body.append(uploadToken).append("\r\n");
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"key\"\r\n\r\n");
        body.append(path).append("\r\n");
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        body.append("Content-Type: application/octet-stream\r\n\r\n");

        byte[] bodyStart = body.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bodyEnd = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] allBytes = new byte[bodyStart.length + bytes.length + bodyEnd.length];
        System.arraycopy(bodyStart, 0, allBytes, 0, bodyStart.length);
        System.arraycopy(bytes, 0, allBytes, bodyStart.length, bytes.length);
        System.arraycopy(bodyEnd, 0, allBytes, bodyStart.length + bytes.length, bodyEnd.length);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .POST(HttpRequest.BodyPublishers.ofByteArray(allBytes))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        log.info("Kodo上传: path={} status={}", path, resp.statusCode());
        if (resp.statusCode() != 200) throw new RuntimeException("Kodo上传失败: " + resp.body());
        return path;
    }

    @Override
    public String getFileUrl(String path, StorageConfig config) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + path;
        }
        return "https://" + config.getEndpoint() + "/" + path;
    }

    @Override public String getPreviewUrl(String path, StorageConfig config) { return getFileUrl(path, config); }

    @Override
    public boolean delete(String path, StorageConfig config) {
        try {
            String host = "rs-" + config.getEndpoint();
            String entry = config.getBucket() + ":" + path;
            String encodedEntry = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(entry.getBytes(StandardCharsets.UTF_8));
            String accessToken = generateAccessToken(config, "/delete/" + encodedEntry + "\n");

            String url = "https://" + host + "/delete/" + encodedEntry;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Authorization", "QBox " + accessToken)
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Kodo删除: path={} status={}", path, resp.statusCode());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.error("Kodo删除失败: path={}", path, e);
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

    private String generateUploadToken(StorageConfig config, String key) throws Exception {
        // 七牛云上传策略
        String putPolicy = "{\"scope\":\"" + config.getBucket() + ":" + key
                + "\",\"deadline\":" + (System.currentTimeMillis() / 1000 + 3600) + "}";
        String encodedPutPolicy = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(putPolicy.getBytes(StandardCharsets.UTF_8));
        String sign = hmacSha1(encodedPutPolicy, config.getSecretKey());
        return config.getAccessKey() + ":" + sign + ":" + encodedPutPolicy;
    }

    private String generateAccessToken(StorageConfig config, String signingStr) throws Exception {
        String sign = hmacSha1(signingStr, config.getSecretKey());
        return config.getAccessKey() + ":" + sign;
    }

    private String hmacSha1(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}