package com.sparkit.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_dict_data")
public class DictData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private String cssClass;
    private String listClass;
    private Integer sort;
    private Integer status;
    private Integer isDefault;
    @TableLogic
    private Integer deleted;
}