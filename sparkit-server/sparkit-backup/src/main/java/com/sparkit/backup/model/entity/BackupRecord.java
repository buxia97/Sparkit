package com.sparkit.backup.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据备份记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_backup_record")
public class BackupRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String backupName;
    private String backupType;
    private String filePath;
    private Long fileSize;
    private String storageType;
    private String status;
    private String dbName;
    private String remark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    /** 远程备份地址 */
    private String remoteUrl;
    @TableLogic
    private Integer deleted;
}