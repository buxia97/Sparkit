package com.sparkit.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 生成记录
 */
@Data
@TableName("sparkit_ai_generation")
public class AiGeneration implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long sessionId;
    private Long modelId;
    private Long userId;
    private String userType;
    private String prompt;
    private String response;
    private String modelType;
    private Integer tokensUsed;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long duration;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
}