package com.sparkit.job.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.job.model.entity.Job;
import com.sparkit.job.model.entity.JobLog;
import com.sparkit.job.service.JobLogService;
import com.sparkit.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理
 */
@Tag(name = "定时任务", description = "定时任务的增删改查、暂停恢复、日志统计")
@RestController
@RequestMapping("/api/v1/admin/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobLogService jobLogService;

    // ============ 任务管理 ============

    @Operation(summary = "任务列表")
    @GetMapping
    public R<PageResult<Job>> list(PageQuery query) {
        return R.ok(jobService.page(query));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public R<Job> getById(@PathVariable Long id) {
        return R.ok(jobService.getById(id));
    }

    @Operation(summary = "创建任务")
    @PostMapping
    public R<?> create(@Valid @RequestBody Job job) {
        jobService.create(job);
        return R.ok();
    }

    @Operation(summary = "更新任务")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Job job) {
        job.setId(id);
        jobService.update(job);
        return R.ok();
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        jobService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "暂停任务")
    @PutMapping("/{id}/pause")
    public R<?> pause(@PathVariable Long id) {
        jobService.pause(id);
        return R.ok();
    }

    @Operation(summary = "恢复任务")
    @PutMapping("/{id}/resume")
    public R<?> resume(@PathVariable Long id) {
        jobService.resume(id);
        return R.ok();
    }

    // ============ 日志与统计 ============

    @Operation(summary = "任务日志列表")
    @GetMapping("/logs")
    public R<PageResult<JobLog>> logList(PageQuery query) {
        return R.ok(jobLogService.page(query));
    }

    @Operation(summary = "任务执行统计")
    @GetMapping("/statistics/count")
    public R<List<Map<String, Object>>> statistics(@RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime) {
        return R.ok(jobLogService.countByJob(startTime, endTime));
    }

    @Operation(summary = "任务成功率")
    @GetMapping("/statistics/success-rate")
    public R<Map<String, Object>> successRate(@RequestParam(required = false) String startTime,
                                               @RequestParam(required = false) String endTime) {
        return R.ok(jobLogService.successRate(startTime, endTime));
    }
}