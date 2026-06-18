package com.sparkit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.job.model.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务 Mapper
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}