package com.sparkit.ai.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.ai.mapper.AiModelMapper;
import com.sparkit.ai.model.entity.AiModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 模型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService extends ServiceImpl<AiModelMapper, AiModel> {

    public List<AiModel> listEnabled() {
        return lambdaQuery().eq(AiModel::getStatus, 1).orderByAsc(AiModel::getSort).list();
    }
}