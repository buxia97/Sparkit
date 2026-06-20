package com.sparkit.notification.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内信
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_site_message")
public class SiteMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String title;
    private String content;
    private String contentType;
    private Integer status;
    private Integer isRead;
    private String extra;
    @TableLogic
    private Integer deleted;
}