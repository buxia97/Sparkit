package com.sparkit.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.constant.CacheKeys;
import com.sparkit.common.constant.Constants;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.payment.mapper.PaymentOrderMapper;
import com.sparkit.payment.model.entity.PaymentOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 支付订单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrderService extends ServiceImpl<PaymentOrderMapper, PaymentOrder> {

    private final RedisTemplate<String, Object> redisTemplate;

    public PageResult<PaymentOrder> page(PageQuery query, Integer status, String channelCode) {
        IPage<PaymentOrder> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PaymentOrder::getStatus, status);
        }
        if (channelCode != null) {
            wrapper.eq(PaymentOrder::getChannelCode, channelCode);
        }
        if (query.getKeyword() != null) {
            wrapper.and(w -> w.eq(PaymentOrder::getOrderNo, query.getKeyword())
                    .or().eq(PaymentOrder::getOutTradeNo, query.getKeyword()));
        }
        wrapper.orderByDesc(PaymentOrder::getCreateTime);
        IPage<PaymentOrder> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 创建支付订单（幂等）
     */
    @Transactional
    public PaymentOrder create(Long userId, String channelCode, BigDecimal amount, String subject,
                                String body, String idempotentKey) {
        // 幂等检查
        String key = CacheKeys.PAYMENT_IDEMPOTENT + idempotentKey;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", Constants.PAYMENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(ErrorCode.PAYMENT_IDEMPOTENT);
        }

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo("PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6));
        order.setUserId(userId);
        order.setChannelCode(channelCode);
        order.setAmount(amount);
        order.setCurrency("CNY");
        order.setSubject(subject);
        order.setBody(body);
        order.setStatus(0); // 待支付
        order.setIdempotentKey(idempotentKey);
        order.setExpireTime(LocalDateTime.now().plusMinutes(Constants.PAYMENT_EXPIRE_MINUTES));
        save(order);

        return order;
    }

    /** 统计 */
    public Map<String, Object> statistics(String startTime, String endTime) {
        return baseMapper.statistics(startTime, endTime);
    }

    /**
     * 支付回调处理
     */
    @Transactional
    public void handleCallback(String channel, Map<String, Object> params) {
        String outTradeNo = (String) params.get("outTradeNo");
        String transactionId = (String) params.get("transactionId");
        boolean success = "SUCCESS".equals(params.get("status"));

        PaymentOrder order = getOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOutTradeNo, outTradeNo));
        if (order == null) {
            log.warn("支付回调订单不存在: outTradeNo={}", outTradeNo);
            return;
        }

        if (order.getStatus() != 0) {
            log.warn("支付回调订单状态异常: outTradeNo={}, status={}", outTradeNo, order.getStatus());
            return;
        }

        order.setStatus(success ? 2 : 3); // 2-已支付 3-支付失败
        order.setPaidTime(java.time.LocalDateTime.now());
        updateById(order);

        log.info("支付回调处理: channel={}, outTradeNo={}, success={}", channel, outTradeNo, success);
    }
}