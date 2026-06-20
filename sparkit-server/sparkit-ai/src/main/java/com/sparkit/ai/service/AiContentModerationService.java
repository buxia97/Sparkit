package com.sparkit.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * AI 内容审核服务
 * 使用 DeepSeek API 对文本、评论等内容进行合规审核
 */
@Slf4j
@Service
public class AiContentModerationService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    /** 敏感词列表（可扩展） */
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            // 可配置化，这里仅示例
            "赌博", "色情", "毒品", "诈骗", "暴力", "恐怖"
    );

    /**
     * 审核文本内容
     * @param content 待审核内容
     * @param apiKey DeepSeek API Key
     * @return 审核结果 {passed: boolean, reason: string, riskLevel: string}
     */
    public Map<String, Object> moderate(String content, String apiKey) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 本地敏感词检查
        String lowerContent = content.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lowerContent.contains(word)) {
                result.put("passed", false);
                result.put("reason", "包含敏感词: " + word);
                result.put("riskLevel", "high");
                result.put("method", "local");
                log.warn("内容审核不通过（本地敏感词）: {}", word);
                return result;
            }
        }

        // 2. AI 审核
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                Map<String, Object> aiResult = moderateWithAI(content, apiKey);
                boolean aiPassed = Boolean.TRUE.equals(aiResult.get("passed"));
                result.putAll(aiResult);
                result.put("method", "ai");
                return result;
            } catch (Exception e) {
                log.warn("AI 审核失败，回退到本地审核: {}", e.getMessage());
            }
        }

        // 3. 默认通过
        result.put("passed", true);
        result.put("reason", "审核通过");
        result.put("riskLevel", "none");
        result.put("method", "local");
        return result;
    }

    /**
     * 使用 AI 进行内容审核
     */
    private Map<String, Object> moderateWithAI(String content, String apiKey) throws Exception {
        String systemPrompt = """
                你是一个内容审核助手。请审核以下内容，判断是否包含违规信息：
                1. 政治敏感内容
                2. 色情低俗内容
                3. 暴力恐怖内容
                4. 违法犯罪内容
                5. 广告欺诈内容
                6. 侮辱谩骂内容

                请以 JSON 格式返回审核结果：
                {"passed": true/false, "reason": "审核意见", "riskLevel": "none/low/medium/high"}
                只返回 JSON，不要包含其他内容。""";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 200);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", "请审核以下内容：\n" + content)
        ));

        String json = MAPPER.writeValueAsString(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = MAPPER.readValue(response.body(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String aiResponse = (String) message.get("content");
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> moderation = MAPPER.readValue(aiResponse.trim(), Map.class);
                    return moderation;
                } catch (Exception e) {
                    return Map.of("passed", true, "reason", "AI 审核结果解析失败", "riskLevel", "unknown");
                }
            }
        }

        return Map.of("passed", true, "reason", "AI 审核超时", "riskLevel", "unknown");
    }

    /**
     * 批量审核
     */
    public List<Map<String, Object>> moderateBatch(List<String> contents, String apiKey) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (String content : contents) {
            results.add(moderate(content, apiKey));
        }
        return results;
    }
}