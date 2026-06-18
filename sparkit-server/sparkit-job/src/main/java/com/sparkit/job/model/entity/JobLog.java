package com.sparkit.job.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务执行日志
 */
@Data
@TableName("sparkit_job_log")
public class JobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long jobId;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String jobMessage;
    private Integer status;
    private String exceptionInfo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private LocalDateTime createTime;
}