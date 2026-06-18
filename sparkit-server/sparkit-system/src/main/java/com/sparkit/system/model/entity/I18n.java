package com.sparkit.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 国际化
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_i18n")
public class I18n extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String i18nKey;
    private String lang;
    private String i18nValue;
}