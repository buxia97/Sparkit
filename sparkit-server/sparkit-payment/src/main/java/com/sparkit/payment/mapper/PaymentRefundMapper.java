package com.sparkit.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.payment.model.entity.PaymentRefund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款 Mapper
 */
@Mapper
public interface PaymentRefundMapper extends BaseMapper<PaymentRefund> {
}