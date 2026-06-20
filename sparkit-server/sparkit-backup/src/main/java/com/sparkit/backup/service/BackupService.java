package com.sparkit.backup.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.backup.mapper.BackupRecordMapper;
import com.sparkit.backup.model.entity.BackupRecord;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 数据备份服务
 * 通过 mysqldump 执行数据库备份，支持本地和远程备份
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService extends ServiceImpl<BackupRecordMapper, BackupRecord> {

    private final ConfigService configService;

    public PageResult<BackupRecord> page(PageQuery query) {
        IPage<BackupRecord> page = new Page<>(query.getPage(), query.getPageSize());
        IPage<BackupRecord> result = page(page,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BackupRecord>()
                        .orderByDesc(BackupRecord::getCreateTime));
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public BackupRecord createBackup(String backupType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupName = "sparkit_backup_" + timestamp + ".sql";
        String backupDir = getBackupDir();
        String filePath = backupDir + File.separator + backupName;

        BackupRecord record = new BackupRecord();
        record.setBackupName(backupName);
        record.setBackupType(backupType != null ? backupType : "full");
        record.setFilePath(filePath);
        record.setStatus("running");
        record.setDbName(getDbName());
        record.setStartTime(LocalDateTime.now());
        save(record);

        CompletableFuture.runAsync(() -> executeBackup(record));

        return record;
    }

    private void executeBackup(BackupRecord record) {
        try {
            Files.createDirectories(Paths.get(getBackupDir()));

            String host = getDbHost();
            String port = getDbPort();
            String user = getDbUser();
            String password = getDbPassword();
            String dbName = getDbName();

            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "-h" + host, "-P" + port, "-u" + user, "-p" + password,
                    "--single-transaction", "--routines", "--triggers",
                    "--result-file=" + record.getFilePath(),
                    dbName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("mysqldump: {}", line);
                }
            }

            int exitCode = process.waitFor();
            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(record.getStartTime(), endTime).getSeconds();

            if (exitCode == 0) {
                File file = new File(record.getFilePath());
                record.setFileSize(file.exists() ? file.length() : 0L);
                record.setStatus("success");
            } else {
                record.setStatus("failed");
                record.setRemark("mysqldump exit code: " + exitCode);
            }
            record.setEndTime(endTime);
            record.setDuration(duration);
            updateById(record);
            log.info("备份完成: {} status={} duration={}s", record.getBackupName(), record.getStatus(), duration);
        } catch (Exception e) {
            log.error("备份失败: {}", record.getBackupName(), e);
            record.setStatus("failed");
            record.setRemark(e.getMessage());
            record.setEndTime(LocalDateTime.now());
            updateById(record);
        }
    }

    @Transactional
    public void restoreBackup(Long id) {
        BackupRecord record = getById(id);
        if (record == null || !"success".equals(record.getStatus())) {
            throw new RuntimeException("备份记录不存在或备份未成功");
        }

        try {
            String host = getDbHost();
            String port = getDbPort();
            String user = getDbUser();
            String password = getDbPassword();
            String dbName = getDbName();

            ProcessBuilder pb = new ProcessBuilder(
                    "mysql", "-h" + host, "-P" + port, "-u" + user, "-p" + password, dbName
            );
            pb.redirectInput(new File(record.getFilePath()));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("恢复失败，mysql exit code: " + exitCode);
            }
            log.info("数据恢复成功: {}", record.getBackupName());
        } catch (Exception e) {
            log.error("数据恢复失败: {}", record.getBackupName(), e);
            throw new RuntimeException("数据恢复失败: " + e.getMessage());
        }
    }

    // ========== 增量备份 ==========

    /**
     * 创建增量备份（基于 mysqlbinlog）
     */
    @Transactional
    public BackupRecord createIncrementalBackup() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupName = "sparkit_incremental_" + timestamp + ".sql";
        String backupDir = getBackupDir();
        String filePath = backupDir + File.separator + backupName;

        BackupRecord record = new BackupRecord();
        record.setBackupName(backupName);
        record.setBackupType("incremental");
        record.setFilePath(filePath);
        record.setStatus("running");
        record.setDbName(getDbName());
        record.setStartTime(LocalDateTime.now());
        save(record);

        CompletableFuture.runAsync(() -> executeIncrementalBackup(record));
        return record;
    }

    private void executeIncrementalBackup(BackupRecord record) {
        try {
            Files.createDirectories(Paths.get(getBackupDir()));

            String host = getDbHost();
            String port = getDbPort();
            String user = getDbUser();
            String password = getDbPassword();

            // 使用 mysqlbinlog 导出增量变更
            ProcessBuilder pb = new ProcessBuilder(
                    "mysqlbinlog",
                    "--read-from-remote-server",
                    "--host=" + host,
                    "--port=" + port,
                    "--user=" + user,
                    "--password=" + password,
                    "--raw",
                    "--result-file=" + record.getFilePath(),
                    "--start-datetime=" + LocalDateTime.now().minusHours(24).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "--stop-datetime=" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("mysqlbinlog: {}", line);
                }
            }

            int exitCode = process.waitFor();
            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(record.getStartTime(), endTime).getSeconds();

            if (exitCode == 0) {
                File file = new File(record.getFilePath());
                record.setFileSize(file.exists() ? file.length() : 0L);
                record.setStatus("success");
            } else {
                record.setStatus("failed");
                record.setRemark("mysqlbinlog exit code: " + exitCode);
            }
            record.setEndTime(endTime);
            record.setDuration(duration);
            updateById(record);
            log.info("增量备份完成: {} status={}", record.getBackupName(), record.getStatus());
        } catch (Exception e) {
            log.error("增量备份失败: {}", record.getBackupName(), e);
            record.setStatus("failed");
            record.setRemark(e.getMessage());
            record.setEndTime(LocalDateTime.now());
            updateById(record);
        }
    }

    // ========== 远程备份（同步到 OSS/S3/FTP） ==========

    /**
     * 将备份文件同步到远程存储（OSS/S3/FTP）
     */
    public void syncToRemote(Long backupId) {
        BackupRecord record = getById(backupId);
        if (record == null || !"success".equals(record.getStatus())) {
            throw new RuntimeException("备份记录不存在或未成功");
        }

        File file = new File(record.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("备份文件不存在: " + record.getFilePath());
        }

        String remoteType = configService.getConfigValue("backup.remote_type");
        if (remoteType == null || remoteType.isBlank()) {
            throw new RuntimeException("未配置远程备份类型");
        }

        try {
            switch (remoteType.toLowerCase()) {
                case "ftp" -> syncToFtp(record, file);
                case "oss" -> syncToOss(record, file);
                case "s3" -> syncToS3(record, file);
                default -> throw new RuntimeException("不支持的远程存储类型: " + remoteType);
            }
            record.setRemoteUrl(remoteType + "://" + record.getBackupName());
            updateById(record);
            log.info("远程备份同步成功: backupId={}, type={}", backupId, remoteType);
        } catch (Exception e) {
            log.error("远程备份同步失败: backupId={}", backupId, e);
            throw new RuntimeException("远程备份同步失败: " + e.getMessage());
        }
    }

    private void syncToFtp(BackupRecord record, File file) throws Exception {
        String host = configService.getConfigValue("backup.ftp.host");
        String port = configService.getConfigValue("backup.ftp.port", "21");
        String user = configService.getConfigValue("backup.ftp.user");
        String password = configService.getConfigValue("backup.ftp.password");
        String remotePath = configService.getConfigValue("backup.ftp.path", "/backup/");

        java.net.URL url = new java.net.URL("ftp://" + user + ":" + password + "@" + host + ":" + port + remotePath + record.getBackupName());
        java.net.URLConnection conn = url.openConnection();
        try (OutputStream os = conn.getOutputStream();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }
    }

    private void syncToOss(BackupRecord record, File file) {
        String endpoint = configService.getConfigValue("backup.oss.endpoint");
        String accessKey = configService.getConfigValue("backup.oss.access_key");
        String secretKey = configService.getConfigValue("backup.oss.secret_key");
        String bucket = configService.getConfigValue("backup.oss.bucket");

        // 阿里云 OSS 真实上传
        try (FileInputStream fis = new FileInputStream(file)) {
            String host = bucket + "." + endpoint;
            String url = "https://" + host + "/" + record.getBackupName();
            java.net.URL ossUrl = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) ossUrl.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(file.length()));

            // 简单签名
            String date = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));
            conn.setRequestProperty("Date", date);
            conn.setRequestProperty("Authorization", "OSS " + accessKey + ":" + signOss("PUT\n\n\n" + date + "\n/" + bucket + "/" + record.getBackupName(), secretKey));

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                conn.getOutputStream().write(buffer, 0, len);
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("OSS 上传失败: " + code);
            }
        } catch (Exception e) {
            throw new RuntimeException("OSS 上传失败: " + e.getMessage(), e);
        }
    }

    private String signOss(String content, String secretKey) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(new javax.crypto.spec.SecretKeySpec(secretKey.getBytes(), "HmacSHA1"));
        return java.util.Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes()));
    }

    private void syncToS3(BackupRecord record, File file) {
        String endpoint = configService.getConfigValue("backup.s3.endpoint");
        String accessKey = configService.getConfigValue("backup.s3.access_key");
        String secretKey = configService.getConfigValue("backup.s3.secret_key");
        String bucket = configService.getConfigValue("backup.s3.bucket");
        String region = configService.getConfigValue("backup.s3.region", "us-east-1");

        // S3 兼容协议上传
        try (FileInputStream fis = new FileInputStream(file)) {
            String url = "https://" + bucket + "." + endpoint + "/" + record.getBackupName();
            java.net.URL s3Url = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) s3Url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(file.length()));
            conn.setRequestProperty("x-amz-acl", "private");

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                conn.getOutputStream().write(buffer, 0, len);
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("S3 上传失败: " + code);
            }
        } catch (Exception e) {
            throw new RuntimeException("S3 上传失败: " + e.getMessage(), e);
        }
    }

    // ========== 定时备份任务 ==========

    /**
     * 定时全量备份（供 Quartz 任务调用）
     */
    public void scheduledFullBackup() {
        log.info("定时全量备份开始");
        BackupRecord record = createBackup("full");
        log.info("定时全量备份已触发: {}", record.getBackupName());
    }

    /**
     * 定时增量备份（供 Quartz 任务调用）
     */
    public void scheduledIncrementalBackup() {
        log.info("定时增量备份开始");
        BackupRecord record = createIncrementalBackup();
        log.info("定时增量备份已触发: {}", record.getBackupName());
    }

    /**
     * 清理过期备份
     */
    public void cleanExpiredBackups(int keepDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(keepDays);
        lambdaUpdate()
                .lt(BackupRecord::getCreateTime, cutoff)
                .remove();
        log.info("清理过期备份: keepDays={}, cutoff={}", keepDays, cutoff);
    }

    // ========== 配置读取 ==========

    private String getBackupDir() {
        return configService.getConfigValue("backup.local_dir", "./backup");
    }

    private String getDbHost() {
        return configService.getConfigValue("spring.datasource.host", "127.0.0.1");
    }

    private String getDbPort() {
        return configService.getConfigValue("spring.datasource.port", "3306");
    }

    private String getDbUser() {
        return configService.getConfigValue("spring.datasource.username", "root");
    }

    private String getDbPassword() {
        return configService.getConfigValue("spring.datasource.password", "");
    }

    private String getDbName() {
        return configService.getConfigValue("spring.datasource.dbname", "sparkit");
    }
}