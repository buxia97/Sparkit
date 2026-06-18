package com.sparkit.notification.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知模板
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_notify_template")
public class NotifyTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String templateCode;
    private String templateName;
    private String notifyType;
    private String title;
    private String content;
    private String params;
    private Integer status;
    @TableLogic
    private Integer deleted;
}