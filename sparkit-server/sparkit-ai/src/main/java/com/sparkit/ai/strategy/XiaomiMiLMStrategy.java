package com.sparkit.ai.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.ai.model.entity.AiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小米大模型策略（MiLM）
 * 使用 OpenAI 兼容接口，API 地址由用户手动配置
 */
@Slf4j
@Component
public class XiaomiMiLMStrategy implements AiModelStrategy {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getProvider() {
        return "xiaomi";
    }

    @Override
    public String getProviderName() {
        return "小米大模型";
    }

    @Override
    public Map<String, Object> chat(String model, List<Map<String, String>> messages, AiModel modelConfig) {
        try {
            String apiBase = getApiBase(modelConfig);
            String apiKey = modelConfig.getApiKey();
            String modelName = model != null ? model : "milm";

            List<Map<String, String>> reqMessages = buildMessages(messages);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("messages", reqMessages);
            body.put("stream", false);
            if (modelConfig.getMaxTokens() != null) {
                body.put("max_tokens", modelConfig.getMaxTokens());
            }
            if (modelConfig.getTemperature() != null) {
                body.put("temperature", modelConfig.getTemperature());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() != 200) {
                log.error("小米大模型 API 调用失败: status={}, body={}", response.statusCode(), responseBody);
                Map<String, Object> errResult = new HashMap<>();
                errResult.put("error", "小米大模型 API 调用失败: " + response.statusCode());
                return errResult;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = MAPPER.readValue(responseBody, Map.class);
            return parseResponse(respMap, modelName);

        } catch (Exception e) {
            log.error("小米大模型对话异常", e);
            Map<String, Object> errResult = new HashMap<>();
            errResult.put("error", e.getMessage());
            return errResult;
        }
    }

    @Override
    public void chatStream(String model, List<Map<String, String>> messages, AiModel modelConfig,
                           java.util.function.Consumer<String> callback) {
        try {
            String apiBase = getApiBase(modelConfig);
            String apiKey = modelConfig.getApiKey();
            String modelName = model != null ? model : "milm";

            List<Map<String, String>> reqMessages = buildMessages(messages);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("messages", reqMessages);
            body.put("stream", true);
            if (modelConfig.getMaxTokens() != null) {
                body.put("max_tokens", modelConfig.getMaxTokens());
            }
            if (modelConfig.getTemperature() != null) {
                body.put("temperature", modelConfig.getTemperature());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(300))
                    .build();

            HttpResponse<java.io.InputStream> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("小米大模型 流式API 调用失败: status={}, body={}", response.statusCode(), errBody);
                callback.accept("[错误] 小米大模型 API 调用失败: " + response.statusCode());
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> chunk = MAPPER.readValue(data, Map.class);
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                if (delta != null && delta.get("content") != null) {
                                    callback.accept((String) delta.get("content"));
                                }
                            }
                        } catch (Exception e) {
                            // 跳过非 JSON 行
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("小米大模型流式对话异常", e);
            callback.accept("[错误] " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, String>> getModels(AiModel modelConfig) {
        return List.of(
                Map.of("id", "milm", "name", "MiLM"),
                Map.of("id", "milm-1.3b", "name", "MiLM-1.3B"),
                Map.of("id", "milm-6b", "name", "MiLM-6B")
        );
    }

    private String getApiBase(AiModel modelConfig) {
        if (modelConfig.getApiBase() != null && !modelConfig.getApiBase().isBlank()) {
            return modelConfig.getApiBase().replaceAll("/+$", "");
        }
        // 小米大模型无默认公开 API，必须由用户手动配置
        throw new RuntimeException("小米大模型需要手动配置 API 地址（apiBase）");
    }

    private List<Map<String, String>> buildMessages(List<Map<String, String>> messages) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            String role = msg.getOrDefault("role", "user");
            String content = msg.getOrDefault("content", "");
            if (content != null && !content.isBlank()) {
                result.add(Map.of("role", role, "content", content));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(Map<String, Object> respMap, String modelName) {
        Map<String, Object> result = new HashMap<>();
        result.put("model", modelName);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message != null) {
                result.put("content", message.getOrDefault("content", ""));
                result.put("role", message.getOrDefault("role", "assistant"));
            }
        }

        Map<String, Object> usage = (Map<String, Object>) respMap.get("usage");
        if (usage != null) {
            result.put("tokens", usage.getOrDefault("total_tokens", 0));
            result.put("promptTokens", usage.getOrDefault("prompt_tokens", 0));
            result.put("completionTokens", usage.getOrDefault("completion_tokens", 0));
        }

        return result;
    }
}