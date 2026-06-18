package com.sparkit.storage.strategy;

import com.sparkit.storage.model.entity.StorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;

/**
 * 七牛云 Kodo 存储策略
 */
@Slf4j
@Component
public class KodoStorageStrategy implements StorageStrategy {

    @Override
    public String getType() {
        return "qiniu-kodo";
    }

    @Override
    public String upload(MultipartFile file, StorageConfig config) throws Exception {
        return upload(file.getBytes(), file.getOriginalFilename(), config);
    }

    @Override
    public String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception {
        String path = buildPath(config, fileName);
        log.info("Kodo上传: bucket={}, path={}", config.getBucket(), path);
        // 实际生产环境使用 qiniu-java-sdk
        return path;
    }

    @Override
    public String getFileUrl(String path, StorageConfig config) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + path;
        }
        return "https://" + config.getEndpoint() + "/" + path;
    }

    @Override
    public String getPreviewUrl(String path, StorageConfig config) {
        return getFileUrl(path, config);
    }

    @Override
    public boolean delete(String path, StorageConfig config) {
        log.info("Kodo删除: bucket={}, path={}", config.getBucket(), path);
        return true;
    }

    @Override
    public InputStream download(String path, StorageConfig config) throws Exception {
        return new URL(getFileUrl(path, config)).openStream();
    }

    @Override
    public boolean exists(String path, StorageConfig config) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(getFileUrl(path, config)).openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildPath(StorageConfig config, String fileName) {
        String base = config.getBasePath() != null ? config.getBasePath() : "";
        return base + "/" + fileName;
    }
}