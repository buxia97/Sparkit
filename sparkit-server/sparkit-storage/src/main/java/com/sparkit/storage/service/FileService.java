package com.sparkit.storage.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.storage.mapper.FileChunkMapper;
import com.sparkit.storage.mapper.FileInfoMapper;
import com.sparkit.storage.model.entity.FileChunk;
import com.sparkit.storage.model.entity.FileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService extends ServiceImpl<FileInfoMapper, FileInfo> {

    private final FileChunkMapper fileChunkMapper;

    @Value("${sparkit.storage.local-path:./uploads}")
    private String localPath;

    @Value("${sparkit.storage.source:local}")
    private String defaultSource;

    /**
     * 文件上传
     */
    @Transactional
    public FileInfo upload(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = FileUtil.extName(originalName);
            String md5 = DigestUtil.md5Hex(file.getBytes());

            // 检查 MD5 是否存在（秒传）
            FileInfo exist = getOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileMd5, md5));
            if (exist != null) {
                return exist;
            }

            // 保存到本地
            String dateDir = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            String relativePath = dateDir + "/" + newFileName;

            File destDir = new File(localPath + "/" + dateDir);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            File destFile = new File(localPath + "/" + relativePath);
            file.transferTo(destFile);

            // 保存记录
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(newFileName);
            fileInfo.setOriginalName(originalName);
            fileInfo.setFilePath(relativePath);
            fileInfo.setFileUrl("/uploads/" + relativePath);
            fileInfo.setFileType(file.getContentType());
            fileInfo.setFileExt(ext);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFileMd5(md5);
            fileInfo.setStorageSource(defaultSource);
            fileInfo.setStatus(1);
            save(fileInfo);

            return fileInfo;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
        }
    }

    /**
     * 分片上传 - 初始化
     */
    public void initChunk(String fileMd5, String fileName, Long fileSize, Integer chunkTotal) {
        // 检查是否已存在相同MD5的文件
        if (count(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileMd5, fileMd5)) > 0) {
            return;
        }
        // 清理旧的分片记录
        fileChunkMapper.delete(new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getFileMd5, fileMd5));
    }

    /**
     * 分片上传
     */
    @Transactional
    public void uploadChunk(String fileMd5, Integer chunkIndex, Integer chunkTotal, MultipartFile chunk) {
        try {
            String chunkMd5 = DigestUtil.md5Hex(chunk.getBytes());

            // 检查分片是否已上传
            if (fileChunkMapper.selectCount(new LambdaQueryWrapper<FileChunk>()
                    .eq(FileChunk::getFileMd5, fileMd5)
                    .eq(FileChunk::getChunkIndex, chunkIndex)) > 0) {
                return;
            }

            // 保存分片
            String chunkDir = localPath + "/chunks/" + fileMd5;
            File dir = new File(chunkDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String chunkPath = chunkDir + "/" + chunkIndex;
            chunk.transferTo(new File(chunkPath));

            FileChunk fileChunk = new FileChunk();
            fileChunk.setFileMd5(fileMd5);
            fileChunk.setChunkIndex(chunkIndex);
            fileChunk.setChunkTotal(chunkTotal);
            fileChunk.setChunkSize(chunk.getSize());
            fileChunk.setChunkMd5(chunkMd5);
            fileChunk.setChunkPath(chunkPath);
            fileChunk.setStatus(1);
            fileChunkMapper.insert(fileChunk);

        } catch (IOException e) {
            log.error("分片上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
        }
    }

    /**
     * 分片合并
     */
    @Transactional
    public FileInfo mergeChunk(String fileMd5, String fileName) {
        // 检查分片是否完整
        Long chunkCount = fileChunkMapper.selectCount(
                new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getFileMd5, fileMd5));
        if (chunkCount == 0) {
            throw new BusinessException(ErrorCode.FILE_CHUNK_INVALID);
        }

        String ext = FileUtil.extName(fileName);
        String dateDir = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = dateDir + "/" + newFileName;

        File destDir = new File(localPath + "/" + dateDir);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File destFile = new File(localPath + "/" + relativePath);
        try (RandomAccessFile raf = new RandomAccessFile(destFile, "rw")) {
            for (int i = 0; i < chunkCount; i++) {
                File chunkFile = new File(localPath + "/chunks/" + fileMd5 + "/" + i);
                byte[] bytes = java.nio.file.Files.readAllBytes(chunkFile.toPath());
                raf.write(bytes);
            }
        } catch (IOException e) {
            log.error("分片合并失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
        }

        // 校验 MD5
        String actualMd5 = DigestUtil.md5Hex(destFile);
        if (!fileMd5.equalsIgnoreCase(actualMd5)) {
            destFile.delete();
            throw new BusinessException(ErrorCode.FILE_MD5_MISMATCH);
        }

        long fileSize = destFile.length();

        // 清理分片
        FileUtil.del(localPath + "/chunks/" + fileMd5);
        fileChunkMapper.delete(new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getFileMd5, fileMd5));

        // 保存文件记录
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(newFileName);
        fileInfo.setOriginalName(fileName);
        fileInfo.setFilePath(relativePath);
        fileInfo.setFileUrl("/uploads/" + relativePath);
        fileInfo.setFileExt(ext);
        fileInfo.setFileSize(fileSize);
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setStorageSource(defaultSource);
        fileInfo.setStatus(1);
        save(fileInfo);

        return fileInfo;
    }

    /**
     * 读取文件字节数据（用于预览/下载）
     */
    public byte[] getFileBytes(FileInfo fileInfo) {
        try {
            String filePath = localPath + "/" + fileInfo.getFilePath();
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("读取文件失败: id={}", fileInfo.getId(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
        }
    }

    /**
     * 管理端文件分页查询
     */
    public PageResult<FileInfo> adminPage(PageQuery query, String keyword, String storageType) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .like(StringUtils.isNotBlank(keyword), FileInfo::getOriginalName, keyword)
                .eq(StringUtils.isNotBlank(storageType), FileInfo::getStorageSource, storageType)
                .orderByDesc(FileInfo::getCreateTime);
        Page<FileInfo> page = page(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }
}