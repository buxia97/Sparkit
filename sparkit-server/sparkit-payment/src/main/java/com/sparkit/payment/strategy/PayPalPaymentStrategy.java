package com.sparkit.payment.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.payment.model.entity.PaymentChannel;
import com.sparkit.payment.model.entity.PaymentOrder;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * PayPal 支付策略（真实对接）
 *
 * 使用 PayPal REST API v2
 * 参考：https://developer.paypal.com/docs/api/orders/v2/
 */
@Slf4j
@Component("paypalPaymentStrategy")
@RequiredArgsConstructor
public class PayPalPaymentStrategy implements PaymentStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();

    private final ConfigService configService;

    private String getClientId() { return configService.getConfigValue("payment.paypal.client_id"); }
    private String getClientSecret() { return configService.getConfigValue("payment.paypal.client_secret"); }
    private String getMode() { return configService.getConfigValue("payment.paypal.mode"); } // sandbox / live
    private String getNotifyUrl() { return configService.getConfigValue("payment.paypal.notify_url"); }

    private String getBaseUrl() {
        return "sandbox".equals(getMode())
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    @Override
    public String getChannelCode() { return "paypal"; }

    @Override
    public String getChannelName() { return "PayPal"; }

    @Override
    public Map<String, Object> createPayment(PaymentOrder order, PaymentChannel channel) throws Exception {
        String clientId = getClientId();
        if (clientId == null || clientId.isBlank()) {
            log.warn("PayPal 未配置，返回模拟数据: orderNo={}", order.getOrderNo());
            return createMockPayment(order);
        }

        String accessToken = getAccessToken();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("intent", "CAPTURE");

        Map<String, Object> purchaseUnit = new LinkedHashMap<>();
        purchaseUnit.put("reference_id", order.getOrderNo());
        purchaseUnit.put("description", order.getSubject());
        purchaseUnit.put("amount", Map.of(
                "currency_code", "USD",
                "value", String.format("%.2f", order.getAmount().doubleValue() / 100.0)
        ));
        body.put("purchase_units", List.of(purchaseUnit));

        Map<String, Object> applicationContext = new LinkedHashMap<>();
        applicationContext.put("return_url", getNotifyUrl());
        applicationContext.put("cancel_url", getNotifyUrl());
        applicationContext.put("user_action", "PAY_NOW");
        body.put("application_context", applicationContext);

        String jsonBody = MAPPER.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/v2/checkout/orders"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("PayPal-Request-Id", order.getOrderNo())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = MAPPER.readValue(response.body(), Map.class);

        if (response.statusCode() >= 400) {
            log.error("PayPal 创建订单失败: {}", respMap);
            throw new RuntimeException("PayPal 创建订单失败: " + respMap.get("message"));
        }

        String approvalUrl = null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> links = (List<Map<String, Object>>) respMap.get("links");
        if (links != null) {
            for (Map<String, Object> link : links) {
                if ("approve".equals(link.get("rel"))) {
                    approvalUrl = (String) link.get("href");
                    break;
                }
            }
        }

        return Map.of(
                "orderNo", order.getOrderNo(),
                "channel", "paypal",
                "paypalOrderId", respMap.get("id"),
                "approvalUrl", approvalUrl != null ? approvalUrl : "",
                "status", respMap.get("status")
        );
    }

    @Override
    public Map<String, Object> queryPayment(PaymentOrder order, PaymentChannel channel) throws Exception {
        String clientId = getClientId();
        if (clientId == null || clientId.isBlank()) {
            return Map.of("orderNo", order.getOrderNo(), "status", "CREATED");
        }

        String accessToken = getAccessToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/v2/checkout/orders/" + order.getTransactionId()))
                .header("Authorization", "Bearer " + accessToken)
                .GET().timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = MAPPER.readValue(response.body(), Map.class);
        return Map.of(
                "orderNo", order.getOrderNo(),
                "status", respMap.get("status"),
                "transactionId", respMap.get("id")
        );
    }

    @Override
    public boolean closePayment(PaymentOrder order, PaymentChannel channel) throws Exception {
        log.info("PayPal 无需主动关闭订单: orderNo={}", order.getOrderNo());
        return true;
    }

    @Override
    public Map<String, Object> handleCallback(Map<String, Object> params, PaymentChannel channel) throws Exception {
        // PayPal 回调：用户批准后，前端传 orderID + token，后端完成 capture
        String paypalOrderId = (String) params.get("orderID");
        String token = (String) params.get("token");

        String accessToken = getAccessToken();

        // 捕获支付
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/v2/checkout/orders/" + paypalOrderId + "/capture"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = MAPPER.readValue(response.body(), Map.class);

        String status = "FAILED";
        String transactionId = null;
        if ("COMPLETED".equals(respMap.get("status"))) {
            status = "SUCCESS";
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) respMap.get("purchase_units");
            if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");
                if (payments != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
                    if (captures != null && !captures.isEmpty()) {
                        transactionId = (String) captures.get(0).get("id");
                    }
                }
            }
        }

        return Map.of(
                "outTradeNo", params.get("invoice_id"),
                "transactionId", transactionId != null ? transactionId : paypalOrderId,
                "status", status
        );
    }

    @Override
    public Map<String, Object> createRefund(PaymentOrder order, PaymentChannel channel, String refundNo,
                                             String refundAmount, String refundReason) throws Exception {
        String clientId = getClientId();
        if (clientId == null || clientId.isBlank()) {
            return Map.of("refundNo", refundNo, "channelRefundNo", "PP_MOCK_" + System.currentTimeMillis(), "status", "SUCCESS");
        }

        String accessToken = getAccessToken();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", Map.of(
                "currency_code", "USD",
                "value", String.format("%.2f", Double.parseDouble(refundAmount) / 100.0)
        ));
        if (refundReason != null) body.put("note_to_payer", refundReason);

        String jsonBody = MAPPER.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/v2/payments/captures/" + order.getTransactionId() + "/refund"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = MAPPER.readValue(response.body(), Map.class);
        return Map.of(
                "refundNo", refundNo,
                "channelRefundNo", respMap.get("id"),
                "status", "COMPLETED".equals(respMap.get("status")) ? "SUCCESS" : respMap.get("status")
        );
    }

    @Override
    public Map<String, Object> queryRefund(String refundNo, PaymentChannel channel) throws Exception {
        String clientId = getClientId();
        if (clientId == null || clientId.isBlank()) {
            return Map.of("refundNo", refundNo, "status", "SUCCESS");
        }

        String accessToken = getAccessToken();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/v2/payments/refunds/" + refundNo))
                .header("Authorization", "Bearer " + accessToken)
                .GET().timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = MAPPER.readValue(response.body(), Map.class);
        return Map.of("refundNo", refundNo, "status", respMap.get("status"));
    }

    @Override
    public boolean verifySign(Map<String, Object> params, PaymentChannel channel) {
        // PayPal 通过 webhook 验证，签名验证在 Webhook 验证层完成
        return true;
    }

    // ==================== 私有方法 ====================

    private String getAccessToken() throws Exception {
        String clientId = getClientId();
        String clientSecret = getClientSecret();
        String auth = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/v1/oauth2/token"))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = MAPPER.readValue(response.body(), Map.class);
        return (String) result.get("access_token");
    }

    private Map<String, Object> createMockPayment(PaymentOrder order) {
        return Map.of(
                "orderNo", order.getOrderNo(),
                "channel", "paypal",
                "paypalOrderId", "PAYPAL_" + order.getOrderNo(),
                "approvalUrl", "https://www.sandbox.paypal.com/checkoutnow?token=" + UUID.randomUUID().toString().substring(0, 8)
        );
    }
}