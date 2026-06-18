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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定时任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService extends ServiceImpl<JobMapper, Job> {

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
    }

    @Transactional
    public void update(Job job) {
        Job exist = getById(job.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        updateById(job);
    }

    /** 暂停任务 */
    public void pause(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        job.setStatus(0);
        updateById(job);
    }

    /** 恢复任务 */
    public void resume(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        job.setStatus(1);
        updateById(job);
    }
}