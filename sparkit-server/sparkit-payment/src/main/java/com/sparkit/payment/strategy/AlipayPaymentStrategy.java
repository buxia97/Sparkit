package com.sparkit.payment.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.payment.model.entity.PaymentChannel;
import com.sparkit.payment.model.entity.PaymentOrder;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 支付宝支付策略（真实对接）
 *
 * 支持：当面付（扫码）/ APP 支付 / 手机网站支付 / 电脑网站支付
 * 参考：https://opendocs.alipay.com/open/
 */
@Slf4j
@Component("alipayPaymentStrategy")
@RequiredArgsConstructor
public class AlipayPaymentStrategy implements PaymentStrategy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();
    private static final String ALIPAY_HOST = "https://openapi.alipay.com/gateway.do";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(java.time.ZoneId.of("Asia/Shanghai"));

    private final ConfigService configService;

    private String getAppId() { return configService.getConfigValue("payment.alipay.app_id"); }
    private String getPrivateKey() { return configService.getConfigValue("payment.alipay.private_key"); }
    private String getAlipayPublicKey() { return configService.getConfigValue("payment.alipay.public_key"); }
    private String getNotifyUrl() { return configService.getConfigValue("payment.alipay.notify_url"); }

    @Override
    public String getChannelCode() { return "alipay"; }

    @Override
    public String getChannelName() { return "支付宝"; }

    @Override
    public Map<String, Object> createPayment(PaymentOrder order, PaymentChannel channel) throws Exception {
        String appId = getAppId();
        if (appId == null || appId.isBlank()) {
            log.warn("支付宝未配置，返回模拟数据: orderNo={}", order.getOrderNo());
            return createMockPayment(order);
        }

        String payMethod = order.getPayMethod() != null ? order.getPayMethod() : "native";

        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", String.format("%.2f", order.getAmount().doubleValue() / 100.0));
        bizContent.put("subject", order.getSubject());
        bizContent.put("product_code", getProductCode(payMethod));

        String method;
        switch (payMethod) {
            case "native":
                method = "alipay.trade.precreate"; // 当面付
                break;
            case "app":
                method = "alipay.trade.app.pay";
                break;
            case "wap":
                method = "alipay.trade.wap.pay";
                break;
            case "page":
                method = "alipay.trade.page.pay";
                bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
                break;
            default:
                method = "alipay.trade.precreate";
        }

        Map<String, String> params = buildCommonParams(method, bizContent);
        String sign = signWithRsa(buildSignString(params));
        params.put("sign", sign);

        if ("native".equals(payMethod)) {
            // 当面付需要 POST 请求
            String formBody = buildFormBody(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ALIPAY_HOST))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(Duration.ofSeconds(15)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> respJson = MAPPER.readValue(response.body(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = (Map<String, Object>) respJson.get("alipay_trade_precreate_response");
            if (resp != null && "10000".equals(resp.get("code"))) {
                return Map.of(
                        "orderNo", order.getOrderNo(),
                        "channel", "alipay",
                        "payMethod", payMethod,
                        "qrCode", resp.get("qr_code")
                );
            }
            throw new RuntimeException("支付宝创建订单失败: " + respJson);
        } else {
            // APP/WAP/网页支付使用 GET 拼接 URL
            String queryString = buildQueryString(params);
            return Map.of(
                    "orderNo", order.getOrderNo(),
                    "channel", "alipay",
                    "payMethod", payMethod,
                    "paymentUrl", ALIPAY_HOST + "?" + queryString
            );
        }
    }

    @Override
    public Map<String, Object> queryPayment(PaymentOrder order, PaymentChannel channel) throws Exception {
        String appId = getAppId();
        if (appId == null || appId.isBlank()) {
            return Map.of("orderNo", order.getOrderNo(), "tradeStatus", "WAIT_BUYER_PAY");
        }

        Map<String, Object> bizContent = Map.of("out_trade_no", order.getOrderNo());
        Map<String, String> params = buildCommonParams("alipay.trade.query", bizContent);
        params.put("sign", signWithRsa(buildSignString(params)));

        String formBody = buildFormBody(params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ALIPAY_HOST))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> respJson = MAPPER.readValue(response.body(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) respJson.get("alipay_trade_query_response");
        if (resp != null) {
            return Map.of(
                    "orderNo", order.getOrderNo(),
                    "tradeStatus", resp.get("trade_status"),
                    "transactionId", resp.get("trade_no"),
                    "buyerLogonId", resp.getOrDefault("buyer_logon_id", "")
            );
        }
        return Map.of("orderNo", order.getOrderNo(), "tradeStatus", "TRADE_CLOSED");
    }

    @Override
    public boolean closePayment(PaymentOrder order, PaymentChannel channel) throws Exception {
        String appId = getAppId();
        if (appId == null || appId.isBlank()) return true;

        Map<String, Object> bizContent = Map.of("out_trade_no", order.getOrderNo());
        Map<String, String> params = buildCommonParams("alipay.trade.close", bizContent);
        params.put("sign", signWithRsa(buildSignString(params)));

        String formBody = buildFormBody(params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ALIPAY_HOST))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(15)).build();
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return true;
    }

    @Override
    public Map<String, Object> handleCallback(Map<String, Object> params, PaymentChannel channel) throws Exception {
        // 支付宝回调为 POST form 参数
        // 先验证签名
        if (!verifySign(params, channel)) {
            log.error("支付宝回调签名验证失败");
            return Map.of("error", "签名验证失败");
        }

        String tradeStatus = (String) params.get("trade_status");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("outTradeNo", params.get("out_trade_no"));
        result.put("transactionId", params.get("trade_no"));
        result.put("buyerLogonId", params.get("buyer_logon_id"));
        result.put("totalAmount", params.get("total_amount"));
        result.put("tradeStatus", tradeStatus);
        return result;
    }

    @Override
    public Map<String, Object> createRefund(PaymentOrder order, PaymentChannel channel, String refundNo,
                                             String refundAmount, String refundReason) throws Exception {
        String appId = getAppId();
        if (appId == null || appId.isBlank()) {
            return Map.of("refundNo", refundNo, "channelRefundNo", "ALI_MOCK_" + System.currentTimeMillis(), "status", "SUCCESS");
        }

        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("refund_amount", String.format("%.2f", Double.parseDouble(refundAmount) / 100.0));
        bizContent.put("out_request_no", refundNo);
        if (refundReason != null) bizContent.put("refund_reason", refundReason);

        Map<String, String> params = buildCommonParams("alipay.trade.refund", bizContent);
        params.put("sign", signWithRsa(buildSignString(params)));

        String formBody = buildFormBody(params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ALIPAY_HOST))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> respJson = MAPPER.readValue(response.body(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) respJson.get("alipay_trade_refund_response");
        if (resp != null && "10000".equals(resp.get("code"))) {
            return Map.of(
                    "refundNo", refundNo,
                    "channelRefundNo", resp.get("trade_no"),
                    "status", "SUCCESS"
            );
        }
        throw new RuntimeException("支付宝退款失败: " + respJson);
    }

    @Override
    public Map<String, Object> queryRefund(String refundNo, PaymentChannel channel) throws Exception {
        String appId = getAppId();
        if (appId == null || appId.isBlank()) {
            return Map.of("refundNo", refundNo, "status", "SUCCESS");
        }

        Map<String, Object> bizContent = Map.of("out_request_no", refundNo);
        Map<String, String> params = buildCommonParams("alipay.trade.fastpay.refund.query", bizContent);
        params.put("sign", signWithRsa(buildSignString(params)));

        String formBody = buildFormBody(params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ALIPAY_HOST))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> respJson = MAPPER.readValue(response.body(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) respJson.get("alipay_trade_fastpay_refund_query_response");
        return Map.of("refundNo", refundNo, "status", resp != null ? resp.get("refund_status") : "UNKNOWN");
    }

    @Override
    public boolean verifySign(Map<String, Object> params, PaymentChannel channel) {
        try {
            String sign = (String) params.remove("sign");
            String signType = (String) params.getOrDefault("sign_type", "RSA2");
            String content = buildSignString(params);

            if ("RSA2".equals(signType)) {
                return verifyRsaSign(content, sign, getAlipayPublicKey());
            }
            return true;
        } catch (Exception e) {
            log.error("支付宝签名验证异常", e);
            return false;
        }
    }

    // ==================== 私有方法 ====================

    private Map<String, String> buildCommonParams(String method, Map<String, Object> bizContent) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", getAppId());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", "UTF-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", TIME_FORMATTER.format(Instant.now()));
        params.put("version", "1.0");
        params.put("notify_url", getNotifyUrl());
        params.put("biz_content", MAPPER.writeValueAsString(bizContent));
        return params;
    }

    private String buildSignString(Map<String, ?> params) {
        String[] sortedKeys = params.keySet().toArray(new String[0]);
        Arrays.sort(sortedKeys);
        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            Object val = params.get(key);
            if (val != null && !val.toString().isEmpty()) {
                sb.append(key).append("=").append(val).append("&");
            }
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    private String signWithRsa(String content) throws Exception {
        String privateKey = getPrivateKey();
        if (privateKey == null) return "MOCK_SIGN";
        privateKey = privateKey.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(kf.generatePrivate(spec));
        sign.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sign.sign());
    }

    private boolean verifyRsaSign(String content, String sign, String publicKey) throws Exception {
        if (publicKey == null) return true;
        publicKey = publicKey.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(kf.generatePublic(spec));
        sig.update(content.getBytes(StandardCharsets.UTF_8));
        return sig.verify(Base64.getDecoder().decode(sign));
    }

    private String buildFormBody(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (sb.length() > 0) sb.append("&");
            sb.append(k).append("=").append(java.net.URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (sb.length() > 0) sb.append("&");
            sb.append(k).append("=").append(java.net.URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private String getProductCode(String payMethod) {
        switch (payMethod) {
            case "app": return "QUICK_MSECURITY_PAY";
            case "wap": return "QUICK_WAP_WAY";
            case "page": return "FAST_INSTANT_TRADE_PAY";
            default: return "FACE_TO_FACE_PAYMENT";
        }
    }

    private Map<String, Object> createMockPayment(PaymentOrder order) {
        return Map.of(
                "orderNo", order.getOrderNo(),
                "channel", "alipay",
                "qrCode", "https://qr.alipay.com/bax" + UUID.randomUUID().toString().substring(0, 8)
        );
    }
}