package com.sparkit.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_config")
public class Config extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String configName;
    private String configKey;
    private String configValue;
    private String configGroup;
    private String valueType;
    private Integer sort;
    private Integer status;
    @TableLogic
    private Integer deleted;
}