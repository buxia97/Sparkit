package com.sparkit.ai.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.ai.mapper.AiMessageMapper;
import com.sparkit.ai.model.entity.AiMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 消息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageService extends ServiceImpl<AiMessageMapper, AiMessage> {

    public List<AiMessage> getBySessionId(Long sessionId) {
        return lambdaQuery()
                .eq(AiMessage::getSessionId, sessionId)
                .orderByAsc(AiMessage::getCreateTime)
                .list();
    }
}