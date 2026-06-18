package com.sparkit.payment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_payment_order")
public class PaymentOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private String outTradeNo;
    private Long userId;
    private String channelCode;
    private String payMethod;
    private BigDecimal amount;
    private BigDecimal refundAmount;
    private String currency;
    private String subject;
    private String body;
    private Integer status;
    private String openid;
    private String transactionId;
    private String notifyUrl;
    private String returnUrl;
    private String idempotentKey;
    private LocalDateTime paidTime;
    private LocalDateTime expireTime;
    @TableLogic
    private Integer deleted;
}