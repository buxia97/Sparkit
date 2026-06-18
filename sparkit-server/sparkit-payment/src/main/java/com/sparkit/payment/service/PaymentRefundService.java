package com.sparkit.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.payment.mapper.PaymentRefundMapper;
import com.sparkit.payment.mapper.PaymentOrderMapper;
import com.sparkit.payment.model.entity.PaymentRefund;
import com.sparkit.payment.model.entity.PaymentOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付退款服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRefundService extends ServiceImpl<PaymentRefundMapper, PaymentRefund> {

    private final PaymentOrderMapper orderMapper;

    /**
     * 退款分页查询
     */
    public PageResult<PaymentRefund> page(PageQuery query, Integer status, String channelCode) {
        Page<PaymentRefund> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<PaymentRefund> wrapper = new LambdaQueryWrapper<PaymentRefund>()
                .eq(status != null, PaymentRefund::getStatus, status)
                .eq(channelCode != null, PaymentRefund::getChannelCode, channelCode)
                .orderByDesc(PaymentRefund::getCreateTime);
        page(page, wrapper);
        return PageResult.of(page);
    }

    /**
     * 创建退款
     */
    @Transactional
    public PaymentRefund createRefund(Long orderId, BigDecimal refundAmount, String refundReason, String channelCode) {
        PaymentOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 检查已退款金额
        BigDecimal refunded = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal maxRefund = order.getAmount().subtract(refunded);
        if (refundAmount.compareTo(maxRefund) > 0) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEED);
        }

        PaymentRefund refund = new PaymentRefund();
        refund.setOrderId(orderId);
        refund.setRefundNo("RFD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6));
        refund.setOutRefundNo(refund.getRefundNo());
        refund.setRefundAmount(refundAmount);
        refund.setRefundReason(refundReason);
        refund.setStatus(0); // 处理中
        refund.setChannelCode(channelCode);
        refund.setRefundTime(LocalDateTime.now());
        save(refund);

        // 更新订单退款金额
        order.setRefundAmount(refunded.add(refundAmount));
        orderMapper.updateById(order);

        log.info("退款创建成功: orderNo={}, refundNo={}, amount={}", order.getOrderNo(), refund.getRefundNo(), refundAmount);
        return refund;
    }

    /**
     * 退款回调处理
     */
    @Transactional
    public void handleRefundCallback(String refundNo, boolean success, String channelRefundNo) {
        PaymentRefund refund = getOne(new LambdaQueryWrapper<PaymentRefund>()
                .eq(PaymentRefund::getRefundNo, refundNo));
        if (refund == null) {
            log.warn("退款记录不存在: refundNo={}", refundNo);
            return;
        }
        refund.setStatus(success ? 1 : 2); // 1-成功 2-失败
        refund.setOutRefundNo(channelRefundNo);
        updateById(refund);
        log.info("退款回调处理: refundNo={}, success={}", refundNo, success);
    }
}