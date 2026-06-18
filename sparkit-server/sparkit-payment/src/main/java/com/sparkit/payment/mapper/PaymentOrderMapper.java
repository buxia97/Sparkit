package com.sparkit.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.payment.model.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 支付订单 Mapper
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /** 统计支付金额（SQL聚合） */
    Map<String, Object> statistics(@Param("startTime") String startTime, @Param("endTime") String endTime);
}