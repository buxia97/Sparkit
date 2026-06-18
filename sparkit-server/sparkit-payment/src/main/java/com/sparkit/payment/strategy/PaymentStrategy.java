package com.sparkit.payment.strategy;

import com.sparkit.payment.model.entity.PaymentChannel;
import com.sparkit.payment.model.entity.PaymentOrder;

import java.util.Map;

/**
 * 支付策略接口
 * 所有支付渠道（微信/支付宝/PayPal/Apple Pay/Google Pay）必须实现此接口
 */
public interface PaymentStrategy {

    /** 获取渠道编码 */
    String getChannelCode();

    /** 创建支付订单，返回支付参数（如二维码链接、JSAPI参数等） */
    Map<String, Object> createPayment(PaymentOrder order, PaymentChannel channel) throws Exception;

    /** 查询支付结果 */
    Map<String, Object> queryPayment(PaymentOrder order, PaymentChannel channel) throws Exception;

    /** 关闭支付订单 */
    boolean closePayment(PaymentOrder order, PaymentChannel channel) throws Exception;

    /** 处理支付回调 */
    Map<String, Object> handleCallback(Map<String, Object> params, PaymentChannel channel) throws Exception;

    /** 申请退款 */
    Map<String, Object> createRefund(PaymentOrder order, PaymentChannel channel, String refundNo,
                                      String refundAmount, String refundReason) throws Exception;

    /** 查询退款结果 */
    Map<String, Object> queryRefund(String refundNo, PaymentChannel channel) throws Exception;

    /** 验证回调签名 */
    boolean verifySign(Map<String, Object> params, PaymentChannel channel);

    /** 获取支付渠道名称 */
    String getChannelName();
}