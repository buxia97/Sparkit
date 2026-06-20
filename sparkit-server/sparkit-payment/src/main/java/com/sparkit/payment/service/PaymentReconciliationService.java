package com.sparkit.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sparkit.payment.mapper.PaymentOrderMapper;
import com.sparkit.payment.model.entity.PaymentChannel;
import com.sparkit.payment.model.entity.PaymentOrder;
import com.sparkit.payment.strategy.PaymentStrategy;
import com.sparkit.payment.strategy.PaymentStrategyFactory;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 支付对账服务
 * 定时对账、差异告警
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentOrderMapper orderMapper;
    private final PaymentChannelService channelService;
    private final PaymentStrategyFactory strategyFactory;
    private final ConfigService configService;

    /**
     * 执行对账（按日期）
     * @param date 对账日期
     * @return 对账结果
     */
    public Map<String, Object> reconcile(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        // 查询该日期所有已支付订单
        List<PaymentOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getStatus, 2) // 已支付
                        .ge(PaymentOrder::getPaidTime, start)
                        .le(PaymentOrder::getPaidTime, end)
        );

        List<Map<String, Object>> differences = new ArrayList<>();
        int totalCount = orders.size();
        int matchCount = 0;
        int diffCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal diffAmount = BigDecimal.ZERO;

        // 按渠道分组
        Map<String, List<PaymentOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(PaymentOrder::getChannelCode));

        for (Map.Entry<String, List<PaymentOrder>> entry : grouped.entrySet()) {
            String channelCode = entry.getKey();
            List<PaymentOrder> channelOrders = entry.getValue();

            try {
                PaymentChannel channel = channelService.getByCode(channelCode);
                PaymentStrategy strategy = strategyFactory.getStrategy(channelCode);

                for (PaymentOrder order : channelOrders) {
                    totalAmount = totalAmount.add(order.getAmount());
                    try {
                        Map<String, Object> result = strategy.queryPayment(order, channel);
                        String tradeState = (String) result.get("tradeState");

                        if ("SUCCESS".equals(tradeState)) {
                            matchCount++;
                        } else {
                            diffCount++;
                            diffAmount = diffAmount.add(order.getAmount());
                            Map<String, Object> diff = new LinkedHashMap<>();
                            diff.put("orderNo", order.getOrderNo());
                            diff.put("channelCode", channelCode);
                            diff.put("localAmount", order.getAmount());
                            diff.put("localStatus", "已支付");
                            diff.put("channelStatus", tradeState);
                            diff.put("paidTime", order.getPaidTime());
                            differences.add(diff);
                            log.warn("对账差异: orderNo={}, local=已支付, channel={}", order.getOrderNo(), tradeState);
                        }
                    } catch (Exception e) {
                        diffCount++;
                        diffAmount = diffAmount.add(order.getAmount());
                        Map<String, Object> diff = new LinkedHashMap<>();
                        diff.put("orderNo", order.getOrderNo());
                        diff.put("channelCode", channelCode);
                        diff.put("localAmount", order.getAmount());
                        diff.put("localStatus", "已支付");
                        diff.put("channelStatus", "查询失败: " + e.getMessage());
                        diff.put("paidTime", order.getPaidTime());
                        differences.add(diff);
                        log.error("对账查询失败: orderNo={}", order.getOrderNo(), e);
                    }
                }
            } catch (Exception e) {
                log.error("渠道对账失败: channelCode={}", channelCode, e);
                for (PaymentOrder o : channelOrders) {
                    diffCount++;
                    diffAmount = diffAmount.add(o.getAmount());
                }
            }
        }

        // 发送差异告警
        if (diffCount > 0) {
            sendAlert(date, diffCount, diffAmount, differences);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());
        result.put("totalCount", totalCount);
        result.put("matchCount", matchCount);
        result.put("diffCount", diffCount);
        result.put("totalAmount", totalAmount.toPlainString());
        result.put("diffAmount", diffAmount.toPlainString());
        result.put("differences", differences);
        return result;
    }

    /**
     * 对账最近 N 天
     */
    public List<Map<String, Object>> reconcileRecent(int days) {
        List<Map<String, Object>> results = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= days; i++) {
            results.add(reconcile(today.minusDays(i)));
        }
        return results;
    }

    /**
     * 发送差异告警
     */
    private void sendAlert(LocalDate date, int diffCount, BigDecimal diffAmount, List<Map<String, Object>> differences) {
        String alertEnabled = configService.getConfigValue("payment.reconciliation.alert_enabled");
        if (!"true".equals(alertEnabled)) {
            return;
        }

        String alertEmail = configService.getConfigValue("payment.reconciliation.alert_email");
        if (alertEmail == null || alertEmail.isBlank()) {
            log.warn("对账差异告警邮件未配置，跳过发送");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【支付对账差异告警】\n");
        sb.append("对账日期: ").append(date).append("\n");
        sb.append("差异订单数: ").append(diffCount).append("\n");
        sb.append("差异金额: ").append(diffAmount.toPlainString()).append(" 元\n");
        sb.append("\n差异详情:\n");
        for (Map<String, Object> diff : differences) {
            sb.append("  - ").append(diff.get("orderNo"))
                    .append(" [").append(diff.get("channelCode")).append("] ")
                    .append(" 本地: ").append(diff.get("localStatus"))
                    .append(" 渠道: ").append(diff.get("channelStatus")).append("\n");
        }

        log.warn("对账差异告警: {}", sb.toString());
        // TODO: 实际发送邮件通知
    }
}