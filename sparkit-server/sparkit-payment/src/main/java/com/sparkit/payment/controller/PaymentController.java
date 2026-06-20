package com.sparkit.payment.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.payment.model.entity.PaymentChannel;
import com.sparkit.payment.model.entity.PaymentOrder;
import com.sparkit.payment.model.entity.PaymentRefund;
import com.sparkit.payment.service.PaymentChannelService;
import com.sparkit.payment.service.PaymentOrderService;
import com.sparkit.payment.service.PaymentReconciliationService;
import com.sparkit.payment.service.PaymentRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付管理
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrderService orderService;
    private final PaymentChannelService channelService;
    private final PaymentRefundService refundService;
    private final PaymentReconciliationService reconciliationService;

    // ============ 订单管理 ============

    @GetMapping("/api/v1/admin/payment/orders")
    public R<PageResult<PaymentOrder>> orderList(PageQuery query,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String channelCode) {
        return R.ok(orderService.page(query, status, channelCode));
    }

    @GetMapping("/api/v1/admin/payment/orders/{id}")
    public R<PaymentOrder> orderGet(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    // ============ 退款管理 ============

    @GetMapping("/api/v1/admin/payment/refunds")
    public R<PageResult<PaymentRefund>> refundList(PageQuery query,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String channelCode) {
        return R.ok(refundService.page(query, status, channelCode));
    }

    @GetMapping("/api/v1/admin/payment/refunds/{id}")
    public R<PaymentRefund> refundGet(@PathVariable Long id) {
        return R.ok(refundService.getById(id));
    }

    @PostMapping("/api/v1/admin/payment/refunds")
    public R<?> refundCreate(@RequestBody Map<String, Object> params) {
        Long orderId = Long.valueOf(params.get("orderId").toString());
        BigDecimal refundAmount = new BigDecimal(params.get("refundAmount").toString());
        String refundReason = (String) params.get("refundReason");
        String channelCode = (String) params.get("channelCode");
        PaymentRefund refund = refundService.createRefund(orderId, refundAmount, refundReason, channelCode);
        return R.ok(refund);
    }

    // ============ 渠道管理 ============

    @GetMapping("/api/v1/admin/payment/channels")
    public R<?> channelList() {
        return R.ok(channelService.list());
    }

    @GetMapping("/api/v1/admin/payment/channels/{id}")
    public R<PaymentChannel> channelGet(@PathVariable Long id) {
        return R.ok(channelService.getById(id));
    }

    @PostMapping("/api/v1/admin/payment/channels")
    public R<?> channelCreate(@RequestBody PaymentChannel channel) {
        channelService.save(channel);
        return R.ok();
    }

    @PutMapping("/api/v1/admin/payment/channels/{id}")
    public R<?> channelUpdate(@PathVariable Long id, @RequestBody PaymentChannel channel) {
        channel.setId(id);
        channelService.updateById(channel);
        return R.ok();
    }

    @DeleteMapping("/api/v1/admin/payment/channels/{id}")
    public R<?> channelDelete(@PathVariable Long id) {
        channelService.removeById(id);
        return R.ok();
    }

    // ============ 支付回调（公开接口） ============

    @PostMapping("/api/v1/public/payment/callback/{channel}")
    public R<?> callback(@PathVariable String channel, @RequestBody Map<String, Object> params) {
        // 根据渠道处理支付回调通知
        orderService.handleCallback(channel, params);
        return R.ok();
    }

    @PostMapping("/api/v1/public/payment/refund-callback/{channel}")
    public R<?> refundCallback(@PathVariable String channel, @RequestBody Map<String, Object> params) {
        // 根据渠道处理退款回调通知
        String refundNo = (String) params.get("refundNo");
        boolean success = "SUCCESS".equals(params.get("status"));
        String channelRefundNo = (String) params.get("channelRefundNo");
        refundService.handleRefundCallback(refundNo, success, channelRefundNo);
        return R.ok();
    }

    // ============ 统计 ============

    @GetMapping("/api/v1/admin/payment/statistics")
    public R<Map<String, Object>> statistics(@RequestParam(required = false) String startTime,
                                              @RequestParam(required = false) String endTime) {
        return R.ok(orderService.statistics(startTime, endTime));
    }

    // ============ 对账管理 ============

    @GetMapping("/api/v1/admin/payment/reconciliation")
    public R<Map<String, Object>> reconcile(@RequestParam String date) {
        return R.ok(reconciliationService.reconcile(java.time.LocalDate.parse(date)));
    }

    @GetMapping("/api/v1/admin/payment/reconciliation/recent")
    public R<List<Map<String, Object>>> reconcileRecent(@RequestParam(defaultValue = "7") int days) {
        return R.ok(reconciliationService.reconcileRecent(days));
    }

    // ============ 虚拟支付 ============

    @PostMapping("/api/v1/admin/payment/virtual-pay")
    public R<Map<String, Object>> virtualPay(@RequestBody Map<String, Object> params) {
        return R.ok(orderService.createVirtualPayment(params));
    }
}