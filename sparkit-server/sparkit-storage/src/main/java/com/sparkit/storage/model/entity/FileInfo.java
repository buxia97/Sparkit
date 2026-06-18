package com.sparkit.storage.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_file")
public class FileInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String fileName;
    private String originalName;
    private String filePath;
    private String fileUrl;
    private String fileType;
    private String fileExt;
    private Long fileSize;
    private String fileMd5;
    private String storageSource;
    private String bucket;
    private Integer status;
    /** 缩略图路径（视频首帧） */
    private String thumbnailPath;
    /** 压缩后文件路径 */
    private String compressedPath;
    @TableLogic
    private Integer deleted;
}