package com.sparkit.storage.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.storage.model.entity.FileInfo;
import com.sparkit.storage.service.FileService;
import com.sparkit.storage.service.MediaProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 文件存储接口
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final MediaProcessService mediaProcessService;

    /** 文件上传 */
    @PostMapping("/api/v1/public/storage/upload")
    public R<FileInfo> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.upload(file));
    }

    /** 初始化分片上传 */
    @PostMapping("/api/v1/public/storage/chunk/init")
    public R<?> initChunk(@RequestBody Map<String, Object> params) {
        fileService.initChunk(
                (String) params.get("fileMd5"),
                (String) params.get("fileName"),
                Long.valueOf(params.get("fileSize").toString()),
                (Integer) params.get("chunkTotal"));
        return R.ok();
    }

    /** 上传分片 */
    @PostMapping("/api/v1/public/storage/chunk/upload")
    public R<?> uploadChunk(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunkTotal") Integer chunkTotal,
            @RequestParam("chunk") MultipartFile chunk) {
        fileService.uploadChunk(fileMd5, chunkIndex, chunkTotal, chunk);
        return R.ok();
    }

    /** 合并分片 */
    @PostMapping("/api/v1/public/storage/chunk/merge")
    public R<FileInfo> mergeChunk(@RequestBody Map<String, String> params) {
        return R.ok(fileService.mergeChunk(params.get("fileMd5"), params.get("fileName")));
    }

    // ============ 管理端文件管理 ============

    /** 文件列表 */
    @GetMapping("/api/v1/admin/storage/files")
    public R<PageResult<FileInfo>> fileList(PageQuery query,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String storageType) {
        return R.ok(fileService.adminPage(query, keyword, storageType));
    }

    /** 删除文件 */
    @DeleteMapping("/api/v1/admin/storage/files/{id}")
    public R<?> fileDelete(@PathVariable Long id) {
        fileService.removeById(id);
        return R.ok();
    }

    /** 文件预览 */
    @GetMapping("/api/v1/public/storage/preview/{id}")
    public void preview(@PathVariable Long id, HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileService.getById(id);
        if (fileInfo == null) {
            response.setStatus(404);
            return;
        }
        // 图片/视频/PDF 直接在线预览，其他类型强制下载
        String contentType = fileInfo.getFileType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" +
                URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8) + "\"");

        try (OutputStream os = response.getOutputStream()) {
            byte[] data = fileService.getFileBytes(fileInfo);
            os.write(data);
            os.flush();
        }
    }

    /** 文件下载 */
    @GetMapping("/api/v1/public/storage/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileService.getById(id);
        if (fileInfo == null) {
            response.setStatus(404);
            return;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8) + "\"");

        try (OutputStream os = response.getOutputStream()) {
            byte[] data = fileService.getFileBytes(fileInfo);
            os.write(data);
            os.flush();
        }
    }

    /** 视频缩略图预览 */
    @GetMapping("/api/v1/public/storage/thumbnail/{id}")
    public void thumbnail(@PathVariable Long id, HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileService.getById(id);
        if (fileInfo == null || fileInfo.getThumbnailPath() == null) {
            response.setStatus(404);
            return;
        }
        response.setContentType("image/jpeg");
        response.setHeader("Content-Disposition", "inline; filename=\"thumb_" +
                URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8) + ".jpg\"");

        try (OutputStream os = response.getOutputStream()) {
            byte[] data = Files.readAllBytes(Paths.get(
                    System.getProperty("user.dir") + "/uploads/" + fileInfo.getThumbnailPath()));
            os.write(data);
            os.flush();
        }
    }
}