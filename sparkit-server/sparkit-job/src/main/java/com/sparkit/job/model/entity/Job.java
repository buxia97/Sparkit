package com.sparkit.job.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_job")
public class Job extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer misfirePolicy;
    private Integer concurrent;
    private Integer status;
    @TableLogic
    private Integer deleted;
}