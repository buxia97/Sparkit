package com.sparkit.backup.controller;

import com.sparkit.backup.model.entity.BackupRecord;
import com.sparkit.backup.service.BackupService;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据备份管理
 */
@Tag(name = "数据备份", description = "全量/增量备份、恢复、远程同步、清理")
@RestController
@RequestMapping("/api/v1/admin/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @Operation(summary = "备份列表")
    @GetMapping("/list")
    public R<PageResult<BackupRecord>> list(PageQuery query) {
        return R.ok(backupService.page(query));
    }

    @Operation(summary = "创建全量备份")
    @PostMapping("/create")
    public R<Map<String, Object>> create(@RequestBody(required = false) Map<String, String> body) {
        String backupType = body != null ? body.getOrDefault("backupType", "full") : "full";
        BackupRecord record = backupService.createBackup(backupType);
        return R.ok(Map.of("id", record.getId(), "backupName", record.getBackupName(), "status", record.getStatus()));
    }

    @Operation(summary = "恢复备份")
    @PostMapping("/restore/{id}")
    public R<?> restore(@PathVariable Long id) {
        backupService.restoreBackup(id);
        return R.ok();
    }

    @Operation(summary = "删除备份")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        backupService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "创建增量备份")
    @PostMapping("/incremental")
    public R<Map<String, Object>> incremental() {
        BackupRecord record = backupService.createIncrementalBackup();
        return R.ok(Map.of("id", record.getId(), "backupName", record.getBackupName(), "status", record.getStatus()));
    }

    @Operation(summary = "同步到远程存储")
    @PostMapping("/sync-to-remote/{id}")
    public R<?> syncToRemote(@PathVariable Long id) {
        backupService.syncToRemote(id);
        return R.ok();
    }

    @Operation(summary = "清理过期备份")
    @PostMapping("/clean")
    public R<?> cleanExpired(@RequestParam(defaultValue = "30") int keepDays) {
        backupService.cleanExpiredBackups(keepDays);
        return R.ok();
    }
}