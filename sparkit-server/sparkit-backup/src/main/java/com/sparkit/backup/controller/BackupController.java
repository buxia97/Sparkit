package com.sparkit.backup.controller;

import com.sparkit.backup.model.entity.BackupRecord;
import com.sparkit.backup.service.BackupService;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据备份管理
 */
@RestController
@RequestMapping("/api/v1/admin/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/list")
    public R<PageResult<BackupRecord>> list(PageQuery query) {
        return R.ok(backupService.page(query));
    }

    @PostMapping("/create")
    public R<Map<String, Object>> create(@RequestBody(required = false) Map<String, String> body) {
        String backupType = body != null ? body.getOrDefault("backupType", "full") : "full";
        BackupRecord record = backupService.createBackup(backupType);
        return R.ok(Map.of("id", record.getId(), "backupName", record.getBackupName(), "status", record.getStatus()));
    }

    @PostMapping("/restore/{id}")
    public R<?> restore(@PathVariable Long id) {
        backupService.restoreBackup(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        backupService.removeById(id);
        return R.ok();
    }
}