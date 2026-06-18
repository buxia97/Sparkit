package com.sparkit.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 模型配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_ai_model")
public class AiModel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String modelCode;
    private String modelName;
    private String provider;
    private String modelType;
    private String apiBase;
    private String apiKey;
    private String apiSecret;
    private String modelVersion;
    private Integer maxTokens;
    private Double temperature;
    private Integer status;
    private Integer sort;
    @TableLogic
    private Integer deleted;
}