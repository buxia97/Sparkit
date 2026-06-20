package com.sparkit.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 租户套餐
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_tenant_package")
public class TenantPackage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String packageName;
    private String packageCode;
    private Integer maxUser;
    private Integer maxStorage;
    private Integer maxMenu;
    private BigDecimal price;
    private Integer duration;
    private Integer status;
    private Integer sort;
    private String remark;
    @TableLogic
    private Integer deleted;
}