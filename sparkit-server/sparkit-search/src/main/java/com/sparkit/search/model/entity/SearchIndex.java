package com.sparkit.search.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 搜索索引配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_search_index")
public class SearchIndex extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String indexName;
    private String indexAlias;
    private String moduleName;
    private String entityClass;
    private String description;
    private Integer status;
    private Integer shards;
    private Integer replicas;
    private String mappingJson;
    @TableLogic
    private Integer deleted;
}