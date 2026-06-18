package com.sparkit.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.ai.model.entity.AiModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型 Mapper
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {
}