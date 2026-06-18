package com.sparkit.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_post")
public class Post extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String postCode;
    private String postName;
    private Integer postSort;
    private Integer status;
    @TableLogic
    private Integer deleted;
}