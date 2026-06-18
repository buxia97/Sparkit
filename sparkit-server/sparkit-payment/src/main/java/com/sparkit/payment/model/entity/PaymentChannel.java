package com.sparkit.payment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付渠道配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_payment_channel")
public class PaymentChannel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String channelCode;
    private String channelName;
    private String appId;
    private String mchId;
    private String apiKey;
    private String privateKey;
    private String publicKey;
    private String notifyUrl;
    private String returnUrl;
    private Integer status;
    private Integer sort;
    @TableLogic
    private Integer deleted;
}