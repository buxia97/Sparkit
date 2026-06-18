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
 * 菜单/权限
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_menu")
public class Menu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private String routerPath;
    private String componentPath;
    private String perms;
    private String icon;
    private Integer sort;
    private Integer status;
    private Integer visible;
    private Integer isCache;
    private Integer isFrame;
    @TableLogic
    private Integer deleted;

    /** 子菜单列表（非数据库字段） */
    @TableField(exist = false)
    private List<Menu> children;
}