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