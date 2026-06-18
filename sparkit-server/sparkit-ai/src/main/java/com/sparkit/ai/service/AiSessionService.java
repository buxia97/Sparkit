package com.sparkit.ai.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.ai.mapper.AiSessionMapper;
import com.sparkit.ai.model.entity.AiSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 会话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSessionService extends ServiceImpl<AiSessionMapper, AiSession> {

    public List<AiSession> listByUserId(Long userId) {
        return lambdaQuery()
                .eq(AiSession::getUserId, userId)
                .orderByDesc(AiSession::getCreateTime)
                .list();
    }
}