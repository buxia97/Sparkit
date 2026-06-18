package com.sparkit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.job.model.entity.JobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 任务日志 Mapper
 */
@Mapper
public interface JobLogMapper extends BaseMapper<JobLog> {

    /** 统计各任务执行次数（SQL聚合，不走列表） */
    List<Map<String, Object>> countByJob(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /** 统计任务成功率 */
    Map<String, Object> successRate(@Param("startTime") String startTime, @Param("endTime") String endTime);
}