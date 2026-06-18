package com.sparkit.storage.strategy;

import cn.hutool.core.io.FileUtil;
import com.sparkit.storage.model.entity.StorageConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地存储策略实现
 */
@Component
public class LocalStorageStrategy implements StorageStrategy {

    @Value("${sparkit.storage.local-path:./uploads}")
    private String baseLocalPath;

    @Override
    public String getType() {
        return "local";
    }

    @Override
    public String upload(MultipartFile file, StorageConfig config) throws Exception {
        return upload(file.getBytes(), file.getOriginalFilename(), config);
    }

    @Override
    public String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception {
        String basePath = config != null && config.getBasePath() != null
                ? config.getBasePath() : baseLocalPath;

        String ext = FileUtil.extName(fileName);
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = dateDir + "/" + newFileName;

        File destDir = new File(basePath + "/" + dateDir);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File destFile = new File(basePath + "/" + relativePath);
        java.nio.file.Files.write(destFile.toPath(), bytes);

        return relativePath;
    }

    @Override
    public String getFileUrl(String path, StorageConfig config) {
        String domain = config != null && config.getDomain() != null
                ? config.getDomain() : "";
        return domain + "/uploads/" + path;
    }

    @Override
    public String getPreviewUrl(String path, StorageConfig config) {
        return getFileUrl(path, config);
    }

    @Override
    public boolean delete(String path, StorageConfig config) {
        String basePath = config != null && config.getBasePath() != null
                ? config.getBasePath() : baseLocalPath;
        File file = new File(basePath + "/" + path);
        return file.exists() && file.delete();
    }

    @Override
    public InputStream download(String path, StorageConfig config) throws Exception {
        String basePath = config != null && config.getBasePath() != null
                ? config.getBasePath() : baseLocalPath;
        File file = new File(basePath + "/" + path);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在: " + path);
        }
        return new FileInputStream(file);
    }

    @Override
    public boolean exists(String path, StorageConfig config) {
        String basePath = config != null && config.getBasePath() != null
                ? config.getBasePath() : baseLocalPath;
        return new File(basePath + "/" + path).exists();
    }
}