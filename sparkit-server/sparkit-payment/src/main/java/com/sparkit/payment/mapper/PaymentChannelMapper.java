package com.sparkit.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.payment.model.entity.PaymentChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付渠道 Mapper
 */
@Mapper
public interface PaymentChannelMapper extends BaseMapper<PaymentChannel> {
}