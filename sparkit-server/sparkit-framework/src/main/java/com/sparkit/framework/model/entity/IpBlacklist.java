package com.sparkit.framework.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IP 黑名单实体
 */
@Data
@TableName("sparkit_ip_blacklist")
public class IpBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** IP 地址 */
    private String ip;

    /** 封禁原因 */
    private String reason;

    /** 封禁时长（分钟），-1 表示永久 */
    private Integer duration;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}