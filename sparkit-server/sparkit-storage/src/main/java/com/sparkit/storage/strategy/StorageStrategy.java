package com.sparkit.storage.strategy;

import com.sparkit.storage.model.entity.StorageConfig;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 存储策略接口
 * 所有存储源（本地/FTP/OSS/COS/Kodo/S3）必须实现此接口
 */
public interface StorageStrategy {

    /** 获取存储源类型 */
    String getType();

    /** 上传文件 */
    String upload(MultipartFile file, StorageConfig config) throws Exception;

    /** 上传文件（字节流） */
    String upload(byte[] bytes, String fileName, StorageConfig config) throws Exception;

    /** 获取文件访问URL */
    String getFileUrl(String path, StorageConfig config);

    /** 获取文件预览URL */
    String getPreviewUrl(String path, StorageConfig config);

    /** 删除文件 */
    boolean delete(String path, StorageConfig config);

    /** 获取文件字节流 */
    InputStream download(String path, StorageConfig config) throws Exception;

    /** 检查文件是否存在 */
    boolean exists(String path, StorageConfig config);
}