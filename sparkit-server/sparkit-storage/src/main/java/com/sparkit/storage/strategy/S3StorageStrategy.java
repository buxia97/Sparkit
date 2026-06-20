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
 * S3 兼容协议存储策略 - 真实 HTTP API 调用（AWS Signature V4）
 * 支持 MinIO / 华为云 OBS / AWS S3 / 其他 S3 兼容存储
 */
@Slf4j
@Component
public class S3StorageStrategy implements StorageStrategy {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private static final String REGION = "us-east-1";
    private static final String SERVICE = "s3";

    @Override public String getType() { return "s3"; }

    @Override
    public String upload(MultipartFile file, StorageConfig config) throws Exception {
        return upload(file.getBytes(), file.getOriginalFilename(), config);
    }

    @Override
    public String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception {
        String path = buildPath(config, fileName);
        String host = buildEndpoint(config);
        String url = host + "/" + config.getBucket() + "/" + path;
        String contentType = getContentType(fileName);
        String payloadHash = sha256Hex(bytes);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT"));
        String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String canonicalUri = "/" + config.getBucket() + "/" + path;
        String canonicalQuery = "";
        String canonicalHeaders = "content-type:" + contentType + "\nhost:" + getHost(config) + "\nx-amz-content-sha256:" + payloadHash + "\nx-amz-date:" + amzDate + "\n";
        String signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";
        String canonicalRequest = "PUT\n" + canonicalUri + "\n" + canonicalQuery + "\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;

        String credentialScope = dateStamp + "/" + REGION + "/" + SERVICE + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = getSignatureKey(config.getAccessKeySecret(), dateStamp, REGION, SERVICE);
        String signature = bytesToHex(hmacSha256(signingKey, stringToSign));

        String auth = "AWS4-HMAC-SHA256 Credential=" + config.getAccessKeyId() + "/" + credentialScope
                + ",SignedHeaders=" + signedHeaders + ",Signature=" + signature;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .header("Content-Type", contentType)
                .header("Host", getHost(config))
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadHash)
                .header("Authorization", auth)
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        log.info("S3上传: path={} status={}", path, resp.statusCode());
        if (resp.statusCode() != 200) throw new RuntimeException("S3上传失败: " + resp.body());
        return path;
    }

    @Override
    public String getFileUrl(String path, StorageConfig config) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + path;
        }
        return buildEndpoint(config) + "/" + config.getBucket() + "/" + path;
    }

    @Override public String getPreviewUrl(String path, StorageConfig config) { return getFileUrl(path, config); }

    @Override
    public boolean delete(String path, StorageConfig config) {
        try {
            String host = buildEndpoint(config);
            String url = host + "/" + config.getBucket() + "/" + path;
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT"));
            String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
            String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String payloadHash = "UNSIGNED-PAYLOAD";

            String canonicalUri = "/" + config.getBucket() + "/" + path;
            String canonicalHeaders = "host:" + getHost(config) + "\nx-amz-date:" + amzDate + "\n";
            String signedHeaders = "host;x-amz-date";
            String canonicalRequest = "DELETE\n" + canonicalUri + "\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;

            String credentialScope = dateStamp + "/" + REGION + "/" + SERVICE + "/aws4_request";
            String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            byte[] signingKey = getSignatureKey(config.getAccessKeySecret(), dateStamp, REGION, SERVICE);
            String signature = bytesToHex(hmacSha256(signingKey, stringToSign));
            String auth = "AWS4-HMAC-SHA256 Credential=" + config.getAccessKeyId() + "/" + credentialScope
                    + ",SignedHeaders=" + signedHeaders + ",Signature=" + signature;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).DELETE()
                    .header("Host", getHost(config)).header("x-amz-date", amzDate)
                    .header("Authorization", auth).timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("S3删除: path={} status={}", path, resp.statusCode());
            return resp.statusCode() == 204;
        } catch (Exception e) {
            log.error("S3删除失败: path={}", path, e);
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

    private String buildEndpoint(StorageConfig config) {
        String ep = config.getEndpoint();
        if (!ep.startsWith("http")) ep = "https://" + ep;
        return ep;
    }

    private String getHost(StorageConfig config) {
        return config.getEndpoint().replace("https://", "").replace("http://", "");
    }

    private byte[] getSignatureKey(String secretKey, String dateStamp, String region, String service) throws Exception {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        return hmacSha256(kService, "aws4_request");
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(data));
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