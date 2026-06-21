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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 支付管理
 */
@Tag(name = "支付管理", description = "支付订单、退款、渠道、对账、虚拟支付")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrderService orderService;
    private final PaymentChannelService channelService;
    private final PaymentRefundService refundService;
    private final PaymentReconciliationService reconciliationService;

    // ============ 订单管理 ============

    @Operation(summary = "订单列表")
    @GetMapping("/api/v1/admin/payment/orders")
    public R<PageResult<PaymentOrder>> orderList(PageQuery query,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String channelCode) {
        return R.ok(orderService.page(query, status, channelCode));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/api/v1/admin/payment/orders/{id}")
    public R<PaymentOrder> orderGet(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    // ============ 退款管理 ============

    @Operation(summary = "退款列表")
    @GetMapping("/api/v1/admin/payment/refunds")
    public R<PageResult<PaymentRefund>> refundList(PageQuery query,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String channelCode) {
        return R.ok(refundService.page(query, status, channelCode));
    }

    @Operation(summary = "获取退款详情")
    @GetMapping("/api/v1/admin/payment/refunds/{id}")
    public R<PaymentRefund> refundGet(@PathVariable Long id) {
        return R.ok(refundService.getById(id));
    }

    @Operation(summary = "创建退款")
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

    @Operation(summary = "渠道列表")
    @GetMapping("/api/v1/admin/payment/channels")
    public R<?> channelList() {
        return R.ok(channelService.list());
    }

    @Operation(summary = "获取渠道详情")
    @GetMapping("/api/v1/admin/payment/channels/{id}")
    public R<PaymentChannel> channelGet(@PathVariable Long id) {
        return R.ok(channelService.getById(id));
    }

    @Operation(summary = "创建渠道")
    @PostMapping("/api/v1/admin/payment/channels")
    public R<?> channelCreate(@RequestBody PaymentChannel channel) {
        channelService.save(channel);
        return R.ok();
    }

    @Operation(summary = "更新渠道")
    @PutMapping("/api/v1/admin/payment/channels/{id}")
    public R<?> channelUpdate(@PathVariable Long id, @RequestBody PaymentChannel channel) {
        channel.setId(id);
        channelService.updateById(channel);
        return R.ok();
    }

    @Operation(summary = "删除渠道")
    @DeleteMapping("/api/v1/admin/payment/channels/{id}")
    public R<?> channelDelete(@PathVariable Long id) {
        channelService.removeById(id);
        return R.ok();
    }

    // ============ 支付回调（公开接口） ============

    @Operation(summary = "支付回调")
    @PostMapping("/api/v1/public/payment/callback/{channel}")
    public R<?> callback(@PathVariable String channel, @RequestBody Map<String, Object> params) {
        orderService.handleCallback(channel, params);
        return R.ok();
    }

    @Operation(summary = "退款回调")
    @PostMapping("/api/v1/public/payment/refund-callback/{channel}")
    public R<?> refundCallback(@PathVariable String channel, @RequestBody Map<String, Object> params) {
        String refundNo = (String) params.get("refundNo");
        boolean success = "SUCCESS".equals(params.get("status"));
        String channelRefundNo = (String) params.get("channelRefundNo");
        refundService.handleRefundCallback(refundNo, success, channelRefundNo);
        return R.ok();
    }

    // ============ 统计 ============

    @Operation(summary = "支付统计")
    @GetMapping("/api/v1/admin/payment/statistics")
    public R<Map<String, Object>> statistics(@RequestParam(required = false) String startTime,
                                              @RequestParam(required = false) String endTime) {
        return R.ok(orderService.statistics(startTime, endTime));
    }

    // ============ 对账管理 ============

    @Operation(summary = "对账")
    @GetMapping("/api/v1/admin/payment/reconciliation")
    public R<Map<String, Object>> reconcile(@RequestParam String date) {
        return R.ok(reconciliationService.reconcile(java.time.LocalDate.parse(date)));
    }

    @Operation(summary = "近期对账")
    @GetMapping("/api/v1/admin/payment/reconciliation/recent")
    public R<List<Map<String, Object>>> reconcileRecent(@RequestParam(defaultValue = "7") int days) {
        return R.ok(reconciliationService.reconcileRecent(days));
    }

    // ============ 虚拟支付 ============

    @Operation(summary = "虚拟支付")
    @PostMapping("/api/v1/admin/payment/virtual-pay")
    public R<Map<String, Object>> virtualPay(@RequestBody Map<String, Object> params) {
        return R.ok(orderService.createVirtualPayment(params));
    }
}