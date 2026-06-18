package com.sparkit.storage.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 存储源配置
 * 支持：本地存储、FTP、阿里云OSS、腾讯云COS、七牛云Kodo、S3兼容协议
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_storage_config")
public class StorageConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 存储源名称 */
    private String name;

    /** 存储源类型: local/ftp/aliyun-oss/tencent-cos/qiniu-kodo/s3 */
    private String storageType;

    /** 存储源编码 */
    private String sourceCode;

    /** 访问域名（CDN/自定义域名） */
    private String domain;

    /** 访问密钥ID */
    private String accessKey;

    /** 访问密钥Secret */
    private String secretKey;

    /** 存储空间/Bucket */
    private String bucket;

    /** 区域/Endpoint */
    private String endpoint;

    /** 存储路径前缀 */
    private String basePath;

    /** 是否默认存储源: 0-否 1-是 */
    private Integer isDefault;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 排序 */
    private Integer sort;

    /** 额外配置（JSON格式） */
    private String extraConfig;

    @TableLogic
    private Integer deleted;
}