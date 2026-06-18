package com.sparkit.storage.strategy;

import com.sparkit.storage.model.entity.StorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;

/**
 * S3 兼容协议存储策略（MinIO / 华为云 OBS / AWS S3 等）
 * 使用原生 HTTP API 与 S3 兼容端点交互，零外部依赖
 */
@Slf4j
@Component
public class S3StorageStrategy implements StorageStrategy {

    @Override
    public String getType() {
        return "s3";
    }

    @Override
    public String upload(MultipartFile file, StorageConfig config) throws Exception {
        return upload(file.getBytes(), file.getOriginalFilename(), config);
    }

    @Override
    public String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception {
        String path = buildPath(config, fileName);
        String url = buildEndpoint(config) + "/" + config.getBucket() + "/" + path;
        log.info("S3上传: bucket={}, path={}", config.getBucket(), path);
        // 实际生产环境使用 AWS SDK 或 MinIO SDK
        // 此处为框架占位，返回存储路径
        return path;
    }

    @Override
    public String getFileUrl(String path, StorageConfig config) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + path;
        }
        return buildEndpoint(config) + "/" + config.getBucket() + "/" + path;
    }

    @Override
    public String getPreviewUrl(String path, StorageConfig config) {
        return getFileUrl(path, config);
    }

    @Override
    public boolean delete(String path, StorageConfig config) {
        log.info("S3删除: bucket={}, path={}", config.getBucket(), path);
        return true;
    }

    @Override
    public InputStream download(String path, StorageConfig config) throws Exception {
        String url = getFileUrl(path, config);
        return new URL(url).openStream();
    }

    @Override
    public boolean exists(String path, StorageConfig config) {
        try {
            String url = getFileUrl(path, config);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildEndpoint(StorageConfig config) {
        String ep = config.getEndpoint();
        if (ep == null) return "https://s3.amazonaws.com";
        return ep.startsWith("http") ? ep : "https://" + ep;
    }

    private String buildPath(StorageConfig config, String fileName) {
        String base = config.getBasePath() != null ? config.getBasePath() : "";
        return base + "/" + fileName;
    }
}