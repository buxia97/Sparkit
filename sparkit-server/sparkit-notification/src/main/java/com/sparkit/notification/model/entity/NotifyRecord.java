package com.sparkit.notification.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知发送记录
 */
@Data
@TableName("sparkit_notify_record")
public class NotifyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String templateCode;
    private String notifyType;
    private String target;
    private String title;
    private String content;
    private Integer status;
    private String errorMsg;
    private Long retryCount;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}