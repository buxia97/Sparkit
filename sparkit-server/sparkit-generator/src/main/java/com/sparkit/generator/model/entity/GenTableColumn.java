package com.sparkit.generator.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 代码生成器 - 列信息
 */
@Data
@TableName("sparkit_gen_table_column")
public class GenTableColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tableId;
    private String columnName;
    private String columnComment;
    private String columnType;
    private String javaType;
    private String javaField;
    private Integer isPk;
    private Integer isIncrement;
    private Integer isRequired;
    private Integer isInsert;
    private Integer isEdit;
    private Integer isList;
    private Integer isQuery;
    private String queryType;
    private String htmlType;
    private String dictType;
    private Integer sort;
}