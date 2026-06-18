package com.sparkit.generator.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码生成器 - 表信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_gen_table")
public class GenTable extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String tableName;
    private String tableComment;
    private String className;
    private String packageName;
    private String moduleName;
    private String businessName;
    private String functionName;
    private String functionAuthor;
    private String genType;
    private String genPath;
    private String options;
    @TableLogic
    private Integer deleted;
}