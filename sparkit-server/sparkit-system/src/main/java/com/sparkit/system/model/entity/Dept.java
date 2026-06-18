package com.sparkit.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 部门
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_dept")
public class Dept extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long parentId;
    private String ancestors;
    private String deptName;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
    @TableLogic
    private Integer deleted;

    /** 子部门列表（非数据库字段） */
    @TableField(exist = false)
    private List<Dept> children;
}