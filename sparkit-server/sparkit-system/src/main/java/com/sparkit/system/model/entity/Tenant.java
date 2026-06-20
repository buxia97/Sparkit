package com.sparkit.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_tenant")
public class Tenant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String tenantName;
    private String tenantCode;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String tenantType;
    private String domain;
    private Long packageId;
    private Integer status;
    private Integer sort;
    private LocalDateTime expireTime;
    @TableLogic
    private Integer deleted;
}