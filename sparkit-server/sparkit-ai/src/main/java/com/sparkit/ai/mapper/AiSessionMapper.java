package com.sparkit.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.ai.model.entity.AiSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话 Mapper
 */
@Mapper
public interface AiSessionMapper extends BaseMapper<AiSession> {
}