package com.sparkit.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.job.mapper.JobLogMapper;
import com.sparkit.job.model.entity.JobLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 任务日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobLogService extends ServiceImpl<JobLogMapper, JobLog> {

    public PageResult<JobLog> page(PageQuery query) {
        IPage<JobLog> page = new Page<>(query.getPage(), query.getPageSize());
        IPage<JobLog> result = lambdaQuery()
                .eq(query.getKeyword() != null, JobLog::getJobName, query.getKeyword())
                .orderByDesc(JobLog::getCreateTime)
                .page(page);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 统计各任务执行次数（SQL聚合） */
    public List<Map<String, Object>> countByJob(String startTime, String endTime) {
        return baseMapper.countByJob(startTime, endTime);
    }

    /** 统计任务成功率 */
    public Map<String, Object> successRate(String startTime, String endTime) {
        return baseMapper.successRate(startTime, endTime);
    }
}