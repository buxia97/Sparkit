package com.sparkit.storage.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分片上传记录
 */
@Data
@TableName("sparkit_file_chunk")
public class FileChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String fileMd5;
    private String fileName;
    private Long fileSize;
    private Integer chunkIndex;
    private Integer chunkTotal;
    private Long chunkSize;
    private String chunkMd5;
    private String chunkPath;
    private String storageSource;
    private Integer status;
    private LocalDateTime createTime;
}