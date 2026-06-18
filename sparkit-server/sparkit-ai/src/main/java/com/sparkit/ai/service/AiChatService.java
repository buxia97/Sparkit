package com.sparkit.ai.service;

import com.sparkit.ai.model.entity.AiGeneration;
import com.sparkit.ai.model.entity.AiMessage;
import com.sparkit.ai.model.entity.AiModel;
import com.sparkit.ai.model.entity.AiSession;
import com.sparkit.ai.strategy.AiModelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AI 对话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final Map<String, AiModelStrategy> strategyMap;
    private final AiSessionService sessionService;
    private final AiMessageService messageService;
    private final AiModelService modelService;
    private final AiGenerationService generationService;

    // 存储活跃的 SSE 连接
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public AiChatService(List<AiModelStrategy> strategies, AiSessionService sessionService,
                         AiMessageService messageService, AiModelService modelService,
                         AiGenerationService generationService) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(AiModelStrategy::getProvider, s -> s));
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.modelService = modelService;
        this.generationService = generationService;
    }

    /**
     * 普通对话
     */
    public Map<String, Object> chat(Long sessionId, String provider, String model, String content) {
        AiModel modelConfig = modelService.lambdaQuery()
                .eq(AiModel::getProvider, provider)
                .eq(AiModel::getStatus, 1)
                .one();
        if (modelConfig == null) {
            throw new RuntimeException("未找到可用的AI模型配置: " + provider);
        }

        AiModelStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            throw new RuntimeException("未找到AI模型策略: " + provider);
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (sessionId != null) {
            List<AiMessage> history = messageService.getBySessionId(sessionId);
            for (AiMessage msg : history) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", content));

        long startTime = System.currentTimeMillis();
        Map<String, Object> result = strategy.chat(model, messages, modelConfig);
        long duration = System.currentTimeMillis() - startTime;

        // 保存用户消息
        if (sessionId != null) {
            AiMessage userMsg = new AiMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("user");
            userMsg.setContent(content);
            userMsg.setCreateTime(LocalDateTime.now());
            messageService.save(userMsg);

            // 保存助手回复
            String reply = (String) result.getOrDefault("content", "");
            if (reply != null && !reply.isBlank()) {
                AiMessage assistantMsg = new AiMessage();
                assistantMsg.setSessionId(sessionId);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(reply);
                assistantMsg.setCreateTime(LocalDateTime.now());
                messageService.save(assistantMsg);
            }
        }

        // 保存生成记录
        AiGeneration generation = new AiGeneration();
        generation.setSessionId(sessionId);
        generation.setModelId(modelConfig.getId());
        generation.setPrompt(content);
        generation.setResponse((String) result.getOrDefault("content", ""));
        generation.setModelType(provider);
        Object tokens = result.get("tokens");
        if (tokens instanceof Number) {
            generation.setTokensUsed(((Number) tokens).intValue());
        }
        Object promptTokens = result.get("promptTokens");
        if (promptTokens instanceof Number) {
            generation.setPromptTokens(((Number) promptTokens).intValue());
        }
        Object completionTokens = result.get("completionTokens");
        if (completionTokens instanceof Number) {
            generation.setCompletionTokens(((Number) completionTokens).intValue());
        }
        generation.setDuration(duration);
        generation.setStatus(result.containsKey("error") ? 0 : 1);
        generation.setErrorMsg((String) result.get("error"));
        generation.setCreateTime(LocalDateTime.now());
        generationService.save(generation);

        return result;
    }

    /**
     * 流式对话（SSE）
     */
    public SseEmitter chatStream(Long sessionId, String provider, String model, String content) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时
        emitters.add(emitter);

        AiModel modelConfig = modelService.lambdaQuery()
                .eq(AiModel::getProvider, provider)
                .eq(AiModel::getStatus, 1)
                .one();
        if (modelConfig == null) {
            try { emitter.send(SseEmitter.event().data("错误: 未找到可用的AI模型配置")); } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        AiModelStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            try { emitter.send(SseEmitter.event().data("错误: 未找到AI模型策略")); } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (sessionId != null) {
            List<AiMessage> history = messageService.getBySessionId(sessionId);
            for (AiMessage msg : history) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", content));

        // 先保存用户消息
        if (sessionId != null) {
            AiMessage userMsg = new AiMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("user");
            userMsg.setContent(content);
            userMsg.setCreateTime(LocalDateTime.now());
            messageService.save(userMsg);
        }

        StringBuilder fullReply = new StringBuilder();
        final Long finalSessionId = sessionId;
        final Long finalModelId = modelConfig.getId();
        final long startTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                strategy.chatStream(model, messages, modelConfig, chunk -> {
                    fullReply.append(chunk);
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        log.error("SSE发送失败", e);
                    }
                });

                // 保存助手回复
                if (finalSessionId != null && fullReply.length() > 0) {
                    AiMessage assistantMsg = new AiMessage();
                    assistantMsg.setSessionId(finalSessionId);
                    assistantMsg.setRole("assistant");
                    assistantMsg.setContent(fullReply.toString());
                    assistantMsg.setCreateTime(LocalDateTime.now());
                    messageService.save(assistantMsg);
                }

                // 保存生成记录
                long duration = System.currentTimeMillis() - startTime;
                AiGeneration generation = new AiGeneration();
                generation.setSessionId(finalSessionId);
                generation.setModelId(finalModelId);
                generation.setPrompt(content);
                generation.setResponse(fullReply.toString());
                generation.setModelType(provider);
                generation.setDuration(duration);
                generation.setStatus(1);
                generation.setCreateTime(LocalDateTime.now());
                generationService.save(generation);

                emitter.complete();
            } catch (Exception e) {
                log.error("流式对话异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        }).start();

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    /**
     * 获取可用模型列表
     */
    public List<Map<String, Object>> getModels() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiModelStrategy strategy : strategyMap.values()) {
            AiModel modelConfig = modelService.lambdaQuery()
                    .eq(AiModel::getProvider, strategy.getProvider())
                    .eq(AiModel::getStatus, 1)
                    .one();
            if (modelConfig == null) {
                continue;
            }
            List<Map<String, String>> models = strategy.getModels(modelConfig);
            for (Map<String, String> model : models) {
                result.add(Map.of(
                        "provider", strategy.getProvider(),
                        "providerName", strategy.getProviderName(),
                        "modelId", model.get("id"),
                        "modelName", model.get("name")
                ));
            }
        }
        return result;
    }

    /**
     * 创建会话
     */
    public AiSession createSession(Long userId, String provider, String sessionName) {
        AiModel modelConfig = modelService.lambdaQuery()
                .eq(AiModel::getProvider, provider)
                .eq(AiModel::getStatus, 1)
                .one();

        AiSession session = new AiSession();
        session.setUserId(userId);
        session.setModelId(modelConfig != null ? modelConfig.getId() : null);
        session.setSessionName(sessionName != null ? sessionName : "新对话");
        session.setStatus(1);
        session.setCreateTime(LocalDateTime.now());
        sessionService.save(session);
        return session;
    }

    /**
     * 获取会话历史消息
     */
    public List<AiMessage> getSessionMessages(Long sessionId) {
        return messageService.getBySessionId(sessionId);
    }
}