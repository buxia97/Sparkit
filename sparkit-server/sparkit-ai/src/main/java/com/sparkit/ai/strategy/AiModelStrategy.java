package com.sparkit.ai.strategy;

import com.sparkit.ai.model.entity.AiModel;

import java.util.Map;

/**
 * AI 模型调用策略接口
 */
public interface AiModelStrategy {

    /** 获取提供商编码 */
    String getProvider();

    /** 获取提供商名称 */
    String getProviderName();

    /**
     * 对话调用
     * @param model 模型名称
     * @param messages 对话消息列表
     * @param modelConfig 模型配置
     * @return 模型响应
     */
    Map<String, Object> chat(String model, java.util.List<Map<String, String>> messages, AiModel modelConfig);

    /**
     * 流式对话调用
     * @param model 模型名称
     * @param messages 对话消息列表
     * @param modelConfig 模型配置实体
     * @param callback 流式回调
     */
    void chatStream(String model, java.util.List<Map<String, String>> messages, AiModel modelConfig,
                    java.util.function.Consumer<String> callback);

    /** 获取可用模型列表 */
    java.util.List<Map<String, String>> getModels(AiModel modelConfig);
}