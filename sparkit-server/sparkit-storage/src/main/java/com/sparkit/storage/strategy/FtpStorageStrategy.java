package com.sparkit.storage.strategy;

import com.sparkit.storage.model.entity.StorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * FTP 远程存储策略
 * 使用 Java 内置 FTP URLConnection 实现，无需外部依赖
 */
@Slf4j
@Component
public class FtpStorageStrategy implements StorageStrategy {

    @Override
    public String getType() {
        return "ftp";
    }

    @Override
    public String upload(MultipartFile file, StorageConfig config) throws Exception {
        return upload(file.getBytes(), file.getOriginalFilename(), config);
    }

    @Override
    public String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception {
        String path = buildPath(config, fileName);
        String ftpUrl = buildFtpUrl(config, path);
        log.info("FTP上传: url={}, path={}", ftpUrl, path);

        URL url = new URL(ftpUrl);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
            os.flush();
        }
        log.info("FTP上传成功: {}", path);
        return path;
    }

    @Override
    public String getFileUrl(String path, StorageConfig config) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + path;
        }
        return "ftp://" + config.getAccessKey() + ":" + config.getSecretKey()
                + "@" + config.getEndpoint() + "/" + path;
    }

    @Override
    public String getPreviewUrl(String path, StorageConfig config) {
        return getFileUrl(path, config);
    }

    @Override
    public boolean delete(String path, StorageConfig config) {
        try {
            String ftpUrl = buildFtpUrl(config, path);
            URL url = new URL(ftpUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            int code = conn.getResponseCode();
            return code == 200 || code == 204;
        } catch (Exception e) {
            log.error("FTP删除失败: {}", path, e);
            return false;
        }
    }

    @Override
    public InputStream download(String path, StorageConfig config) throws Exception {
        String ftpUrl = buildFtpUrl(config, path);
        URL url = new URL(ftpUrl);
        URLConnection conn = url.openConnection();
        return conn.getInputStream();
    }

    @Override
    public boolean exists(String path, StorageConfig config) {
        try {
            String ftpUrl = buildFtpUrl(config, path);
            URL url = new URL(ftpUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildFtpUrl(StorageConfig config, String path) {
        String userInfo = config.getAccessKey() != null
                ? config.getAccessKey() + ":" + (config.getSecretKey() != null ? config.getSecretKey() : "")
                : "anonymous:anonymous";
        return "ftp://" + userInfo + "@" + config.getEndpoint() + "/" + path;
    }

    private String buildPath(StorageConfig config, String fileName) {
        String base = config.getBasePath() != null ? config.getBasePath() : "";
        return base + "/" + fileName;
    }
}