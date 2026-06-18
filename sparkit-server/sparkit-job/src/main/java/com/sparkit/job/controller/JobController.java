package com.sparkit.job.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.job.model.entity.Job;
import com.sparkit.job.model.entity.JobLog;
import com.sparkit.job.service.JobLogService;
import com.sparkit.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理
 */
@RestController
@RequestMapping("/api/v1/admin/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobLogService jobLogService;

    // ============ 任务管理 ============

    @GetMapping
    public R<PageResult<Job>> list(PageQuery query) {
        return R.ok(jobService.page(query));
    }

    @GetMapping("/{id}")
    public R<Job> getById(@PathVariable Long id) {
        return R.ok(jobService.getById(id));
    }

    @PostMapping
    public R<?> create(@Valid @RequestBody Job job) {
        jobService.create(job);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Job job) {
        job.setId(id);
        jobService.update(job);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        jobService.removeById(id);
        return R.ok();
    }

    @PutMapping("/{id}/pause")
    public R<?> pause(@PathVariable Long id) {
        jobService.pause(id);
        return R.ok();
    }

    @PutMapping("/{id}/resume")
    public R<?> resume(@PathVariable Long id) {
        jobService.resume(id);
        return R.ok();
    }

    // ============ 日志与统计 ============

    @GetMapping("/logs")
    public R<PageResult<JobLog>> logList(PageQuery query) {
        return R.ok(jobLogService.page(query));
    }

    @GetMapping("/statistics/count")
    public R<List<Map<String, Object>>> statistics(@RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime) {
        return R.ok(jobLogService.countByJob(startTime, endTime));
    }

    @GetMapping("/statistics/success-rate")
    public R<Map<String, Object>> successRate(@RequestParam(required = false) String startTime,
                                               @RequestParam(required = false) String endTime) {
        return R.ok(jobLogService.successRate(startTime, endTime));
    }
}