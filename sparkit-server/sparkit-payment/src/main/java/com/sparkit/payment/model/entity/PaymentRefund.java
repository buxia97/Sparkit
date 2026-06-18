package com.sparkit.payment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付退款
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_payment_refund")
public class PaymentRefund extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private String refundNo;
    private String outRefundNo;
    private BigDecimal refundAmount;
    private String refundReason;
    private Integer status;
    private String channelCode;
    private LocalDateTime refundTime;
}