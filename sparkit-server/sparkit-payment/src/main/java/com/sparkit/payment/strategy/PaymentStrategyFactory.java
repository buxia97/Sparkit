package com.sparkit.payment.strategy;

import com.sparkit.payment.model.entity.PaymentChannel;
import com.sparkit.payment.model.entity.PaymentOrder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付策略工厂
 */
@Component
public class PaymentStrategyFactory {

    private final Map<String, PaymentStrategy> strategyMap;

    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::getChannelCode, s -> s));
    }

    public PaymentStrategy getStrategy(String channelCode) {
        PaymentStrategy strategy = strategyMap.get(channelCode);
        if (strategy == null) {
            throw new RuntimeException("未找到支付策略: " + channelCode);
        }
        return strategy;
    }
}