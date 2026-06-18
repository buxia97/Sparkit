package com.sparkit.user.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 社交登录策略工厂
 */
@Component
@RequiredArgsConstructor
public class SocialLoginStrategyFactory {

    private final Map<String, SocialLoginStrategy> strategyMap;

    public SocialLoginStrategy getStrategy(String platform) {
        SocialLoginStrategy strategy = strategyMap.get(platform + "LoginStrategy");
        if (strategy == null) {
            throw new RuntimeException("不支持的社交登录平台: " + platform);
        }
        return strategy;
    }

    /** 获取所有已配置的平台列表 */
    public List<String> getAvailablePlatforms() {
        return strategyMap.keySet().stream()
                .map(k -> k.replace("LoginStrategy", ""))
                .collect(Collectors.toList());
    }
}