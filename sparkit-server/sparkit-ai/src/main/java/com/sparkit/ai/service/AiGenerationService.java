package com.sparkit.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.ai.mapper.AiGenerationMapper;
import com.sparkit.ai.model.entity.AiGeneration;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 生成记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationService extends ServiceImpl<AiGenerationMapper, AiGeneration> {

    public PageResult<AiGeneration> page(PageQuery query) {
        IPage<AiGeneration> page = new Page<>(query.getPage(), query.getPageSize());
        IPage<AiGeneration> result = lambdaQuery()
                .orderByDesc(AiGeneration::getCreateTime)
                .page(page);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 按模型统计 */
    public List<Map<String, Object>> countByModel(String startTime, String endTime) {
        return baseMapper.countByModel(startTime, endTime);
    }
}