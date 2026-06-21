package com.sparkit.storage.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 存储策略工厂
 * 根据存储源类型自动选择对应的策略实现
 */
@Component
public class StorageStrategyFactory {

    private final Map<String, StorageStrategy> strategyMap;

    public StorageStrategyFactory(List<StorageStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(StorageStrategy::getType, s -> s));
    }

    /** 获取指定类型的存储策略 */
    public StorageStrategy getStrategy(String storageType) {
        StorageStrategy strategy = strategyMap.get(storageType);
        if (strategy == null) {
            // 默认返回本地存储
            strategy = strategyMap.get("local");
        }
        if (strategy == null) {
            throw new RuntimeException("未找到存储策略: " + storageType);
        }
        return strategy;
    }
}