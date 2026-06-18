package com.sparkit.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * 媒体处理服务
 * 图片压缩 + 视频转码、首帧提取
 */
@Slf4j
@Service
public class MediaProcessService {

    @Value("${sparkit.storage.local-path:./uploads}")
    private String localPath;

    /** ffmpeg 是否可用 */
    private volatile Boolean ffmpegAvailable = null;

    /**
     * 检查 ffmpeg 是否可用（缓存结果）
     */
    public boolean isFfmpegAvailable() {
        if (ffmpegAvailable != null) {
            return ffmpegAvailable;
        }
        synchronized (this) {
            if (ffmpegAvailable != null) {
                return ffmpegAvailable;
            }
            try {
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                ffmpegAvailable = finished && process.exitValue() == 0;
                if (ffmpegAvailable) {
                    log.info("ffmpeg 已检测到，视频处理功能可用");
                } else {
                    log.warn("ffmpeg 未安装或不可用，视频处理功能将跳过");
                }
            } catch (Exception e) {
                ffmpegAvailable = false;
                log.warn("ffmpeg 检测失败，视频处理功能将跳过: {}", e.getMessage());
            }
        }
        return ffmpegAvailable;
    }

    /**
     * 图片压缩
     * @param sourcePath 源文件绝对路径
     * @param quality 压缩质量 0.0-1.0
     * @return 压缩后的文件路径（相对路径），失败返回 null
     */
    public String compressImage(String sourcePath, float quality) {
        if (quality <= 0 || quality >= 1.0f) {
            return null; // 质量0或1不需要压缩
        }

        Path sourceFile = Paths.get(sourcePath);
        if (!Files.exists(sourceFile)) {
            log.warn("图片压缩: 源文件不存在 {}", sourcePath);
            return null;
        }

        String ext = getExtension(sourcePath).toLowerCase();
        // 只压缩 JPEG 和 PNG
        if (!"jpg".equals(ext) && !"jpeg".equals(ext) && !"png".equals(ext)) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(sourceFile.toFile());
            if (image == null) {
                log.warn("图片压缩: 无法读取图片 {}", sourcePath);
                return null;
            }

            String compressedFileName = getBaseName(sourcePath) + "_compressed." + ext;
            String relativePath = getRelativePath(sourcePath);
            String compressedRelativePath = relativePath.replace(getFileName(sourcePath), compressedFileName);
            Path compressedFile = Paths.get(localPath, compressedRelativePath);

            Files.createDirectories(compressedFile.getParent());

            // JPEG 压缩
            if ("jpg".equals(ext) || "jpeg".equals(ext)) {
                compressJpeg(image, compressedFile.toFile(), quality);
            } else {
                // PNG 压缩：转成 JPEG 以减小体积
                String jpgName = getBaseName(sourcePath) + "_compressed.jpg";
                String jpgRelativePath = relativePath.replace(getFileName(sourcePath), jpgName);
                Path jpgFile = Paths.get(localPath, jpgRelativePath);
                Files.createDirectories(jpgFile.getParent());
                compressJpeg(image, jpgFile.toFile(), quality);
                compressedRelativePath = jpgRelativePath;
            }

            long originalSize = Files.size(sourceFile);
            long compressedSize = Files.size(compressedFile);
            log.info("图片压缩完成: {} -> {} ({}KB -> {}KB, quality={})",
                    getFileName(sourcePath), getFileName(compressedRelativePath),
                    originalSize / 1024, compressedSize / 1024, quality);

            return compressedRelativePath;
        } catch (Exception e) {
            log.error("图片压缩失败: {}", sourcePath, e);
            return null;
        }
    }

    /**
     * JPEG 压缩
     */
    private void compressJpeg(BufferedImage image, File output, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /**
     * 视频转码（H.264）
     * @param sourcePath 源文件绝对路径
     * @return 转码后的文件路径（相对路径），失败返回 null
     */
    public String transcodeVideo(String sourcePath) {
        if (!isFfmpegAvailable()) {
            log.info("ffmpeg 不可用，跳过视频转码: {}", sourcePath);
            return null;
        }

        Path sourceFile = Paths.get(sourcePath);
        if (!Files.exists(sourceFile)) {
            log.warn("视频转码: 源文件不存在 {}", sourcePath);
            return null;
        }

        String ext = getExtension(sourcePath).toLowerCase();
        String baseName = getBaseName(sourcePath);
        String transcodedFileName = baseName + "_transcoded.mp4";
        String relativePath = getRelativePath(sourcePath);
        String transcodedRelativePath = relativePath.replace(getFileName(sourcePath), transcodedFileName);
        Path transcodedFile = Paths.get(localPath, transcodedRelativePath);

        try {
            Files.createDirectories(transcodedFile.getParent());

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", sourceFile.toAbsolutePath().toString(),
                    "-c:v", "libx264",
                    "-preset", "fast",
                    "-crf", "23",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    transcodedFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("视频转码超时: {}", sourcePath);
                return null;
            }
            if (process.exitValue() != 0) {
                String errorOutput = new String(process.getInputStream().readAllBytes());
                log.error("视频转码失败: exitCode={}, output={}", process.exitValue(), errorOutput);
                return null;
            }

            long originalSize = Files.size(sourceFile);
            long transcodedSize = Files.size(transcodedFile);
            log.info("视频转码完成: {} -> {} ({}MB -> {}MB)",
                    getFileName(sourcePath), transcodedFileName,
                    originalSize / 1024 / 1024, transcodedSize / 1024 / 1024);

            return transcodedRelativePath;
        } catch (Exception e) {
            log.error("视频转码异常: {}", sourcePath, e);
            return null;
        }
    }

    /**
     * 提取视频第一帧
     * @param sourcePath 源文件绝对路径
     * @return 缩略图相对路径，失败返回 null
     */
    public String extractFirstFrame(String sourcePath) {
        if (!isFfmpegAvailable()) {
            log.info("ffmpeg 不可用，跳过视频首帧提取: {}", sourcePath);
            return null;
        }

        Path sourceFile = Paths.get(sourcePath);
        if (!Files.exists(sourceFile)) {
            log.warn("视频首帧提取: 源文件不存在 {}", sourcePath);
            return null;
        }

        String baseName = getBaseName(sourcePath);
        String thumbnailFileName = baseName + "_thumb.jpg";
        String relativePath = getRelativePath(sourcePath);
        String thumbnailRelativePath = relativePath.replace(getFileName(sourcePath), thumbnailFileName);
        Path thumbnailFile = Paths.get(localPath, thumbnailRelativePath);

        try {
            Files.createDirectories(thumbnailFile.getParent());

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", sourceFile.toAbsolutePath().toString(),
                    "-vframes", "1",
                    "-f", "image2",
                    "-q:v", "2",
                    thumbnailFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("视频首帧提取超时: {}", sourcePath);
                return null;
            }
            if (process.exitValue() != 0 || !Files.exists(thumbnailFile)) {
                log.error("视频首帧提取失败: exitCode={}", process.exitValue());
                return null;
            }

            log.info("视频首帧提取完成: {} -> {}", getFileName(sourcePath), thumbnailFileName);
            return thumbnailRelativePath;
        } catch (Exception e) {
            log.error("视频首帧提取异常: {}", sourcePath, e);
            return null;
        }
    }

    /**
     * 检查是否为图片类型
     */
    public boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 检查是否为视频类型
     */
    public boolean isVideo(String contentType) {
        return contentType != null && contentType.startsWith("video/");
    }

    private String getExtension(String path) {
        int dotIndex = path.lastIndexOf('.');
        return dotIndex > 0 ? path.substring(dotIndex + 1) : "";
    }

    private String getBaseName(String path) {
        int dotIndex = path.lastIndexOf('.');
        return dotIndex > 0 ? path.substring(0, dotIndex) : path;
    }

    private String getFileName(String path) {
        int slashIndex = path.lastIndexOf('/');
        if (slashIndex < 0) slashIndex = path.lastIndexOf('\\');
        return slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
    }

    private String getRelativePath(String absolutePath) {
        String normalizedLocal = localPath.replace('\\', '/');
        String normalizedAbs = absolutePath.replace('\\', '/');
        if (normalizedAbs.startsWith(normalizedLocal)) {
            String rel = normalizedAbs.substring(normalizedLocal.length());
            if (rel.startsWith("/")) {
                rel = rel.substring(1);
            }
            return rel;
        }
        return absolutePath;
    }
}