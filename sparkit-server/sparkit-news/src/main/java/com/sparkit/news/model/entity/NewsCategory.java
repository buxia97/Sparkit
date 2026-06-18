package com.sparkit.news.model.entity;

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
 * 新闻分类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_news_category")
public class NewsCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long parentId;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private Integer sort;
    private Integer status;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private List<NewsCategory> children;
}