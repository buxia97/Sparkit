package com.sparkit.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.ai.model.entity.AiGeneration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * AI 生成记录 Mapper
 */
@Mapper
public interface AiGenerationMapper extends BaseMapper<AiGeneration> {

    /** 按模型统计生成量 */
    List<Map<String, Object>> countByModel(@Param("startTime") String startTime, @Param("endTime") String endTime);
}