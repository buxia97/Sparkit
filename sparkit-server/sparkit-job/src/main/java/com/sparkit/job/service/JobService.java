package com.sparkit.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.job.mapper.JobMapper;
import com.sparkit.job.model.entity.Job;
import com.sparkit.job.quartz.QuartzManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定时任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService extends ServiceImpl<JobMapper, Job> {

    private final QuartzManager quartzManager;

    public PageResult<Job> page(PageQuery query) {
        IPage<Job> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null) {
            wrapper.like(Job::getJobName, query.getKeyword())
                    .or().like(Job::getInvokeTarget, query.getKeyword());
        }
        wrapper.orderByAsc(Job::getCreateTime);
        IPage<Job> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void create(Job job) {
        if (count(new LambdaQueryWrapper<Job>()
                .eq(Job::getJobName, job.getJobName())
                .eq(Job::getJobGroup, job.getJobGroup())) > 0) {
            throw new BusinessException(ErrorCode.JOB_EXISTS);
        }
        save(job);
        try {
            quartzManager.scheduleJob(job);
        } catch (SchedulerException e) {
            log.error("Quartz 调度失败: jobId={}", job.getId(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "任务调度失败: " + e.getMessage());
        }
    }

    @Transactional
    public void update(Job job) {
        Job exist = getById(job.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        updateById(job);
        try {
            quartzManager.scheduleJob(job);
        } catch (SchedulerException e) {
            log.error("Quartz 调度更新失败: jobId={}", job.getId(), e);
        }
    }

    /** 暂停任务 */
    public void pause(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        job.setStatus(0);
        updateById(job);
        try {
            quartzManager.pauseJob(id);
        } catch (SchedulerException e) {
            log.error("Quartz 暂停失败: jobId={}", id, e);
        }
    }

    /** 恢复任务 */
    public void resume(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        job.setStatus(1);
        updateById(job);
        try {
            quartzManager.resumeJob(id);
        } catch (SchedulerException e) {
            log.error("Quartz 恢复失败: jobId={}", id, e);
        }
    }

    /** 立即执行一次 */
    public void runOnce(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        try {
            quartzManager.runOnce(id);
        } catch (SchedulerException e) {
            log.error("Quartz 执行失败: jobId={}", id, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "任务执行失败: " + e.getMessage());
        }
    }
}