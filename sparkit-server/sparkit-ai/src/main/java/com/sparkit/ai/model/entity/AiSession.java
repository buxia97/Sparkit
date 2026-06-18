package com.sparkit.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 会话
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_ai_session")
public class AiSession extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long modelId;
    private String sessionName;
    private String userType;
    private Integer status;
    @TableLogic
    private Integer deleted;
}