package com.sparkit.payment.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.payment.mapper.PaymentChannelMapper;
import com.sparkit.payment.model.entity.PaymentChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支付渠道服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentChannelService extends ServiceImpl<PaymentChannelMapper, PaymentChannel> {

    public List<PaymentChannel> listEnabled() {
        return lambdaQuery().eq(PaymentChannel::getStatus, 1).orderByAsc(PaymentChannel::getSort).list();
    }

    public PaymentChannel getByCode(String channelCode) {
        return lambdaQuery().eq(PaymentChannel::getChannelCode, channelCode).one();
    }
}