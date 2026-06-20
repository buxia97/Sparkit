package com.sparkit.news.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkit.news.model.entity.NewsArticle;
import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * 新闻 AI 服务
 * 支持 AI 采集摘要、AI 生成内容
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAiService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60)).build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConfigService configService;
    private final NewsArticleService articleService;

    private String getAiApiKey() {
        return configService.getConfigValue("news.ai.deepseek.api_key");
    }

    private String getAiApiBase() {
        return configService.getConfigValue("news.ai.deepseek.api_base", "https://api.deepseek.com");
    }

    /**
     * AI 生成文章摘要
     */
    public String generateSummary(NewsArticle article) {
        String apiKey = getAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI API Key 未配置，使用自动截取摘要");
            return autoSummary(article.getContent());
        }
        try {
            String content = article.getContent();
            if (content == null || content.length() < 100) {
                return autoSummary(content);
            }

            // 截取前 2000 字符避免 token 超限
            String truncated = content.length() > 2000 ? content.substring(0, 2000) : content;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.3);
            body.put("max_tokens", 200);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", "你是一个新闻编辑。请为以下新闻生成一段 100 字以内的中文摘要，只返回摘要内容，不要加任何前缀。"),
                    Map.of("role", "user", "content", truncated)
            ));

            String json = MAPPER.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getAiApiBase() + "/v1/chat/completions"))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode result = MAPPER.readTree(resp.body());
            String summary = result.get("choices").get(0).get("message").get("content").asText().trim();
            log.info("AI 摘要生成成功: articleId={}", article.getId());
            return summary;
        } catch (Exception e) {
            log.error("AI 摘要生成失败: articleId={}", article.getId(), e);
            return autoSummary(article.getContent());
        }
    }

    /**
     * AI 生成新闻内容
     */
    public String generateContent(String topic, String style, int wordCount) {
        String apiKey = getAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("AI API Key 未配置");
        }
        try {
            String prompt = String.format(
                    "请以%s风格写一篇关于「%s」的新闻文章，字数约%d字。要求：标题吸引人，内容结构清晰，包含导语、正文和小结。",
                    style != null ? style : "正式新闻", topic, wordCount);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.7);
            body.put("max_tokens", Math.min(wordCount * 2, 4000));
            body.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));

            String json = MAPPER.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getAiApiBase() + "/v1/chat/completions"))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode result = MAPPER.readTree(resp.body());
            String content = result.get("choices").get(0).get("message").get("content").asText().trim();
            log.info("AI 内容生成成功: topic={}", topic);
            return content;
        } catch (Exception e) {
            log.error("AI 内容生成失败: topic={}", topic, e);
            throw new RuntimeException("AI 内容生成失败: " + e.getMessage());
        }
    }

    /**
     * AI 采集网页摘要
     */
    public String collectSummary(String url) {
        String apiKey = getAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("AI API Key 未配置");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.3);
            body.put("max_tokens", 300);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", "你是一个内容采集助手。请访问以下 URL 并提取其中新闻文章的核心内容，生成 200 字以内的摘要。"),
                    Map.of("role", "user", "content", "请采集并总结以下 URL 的内容：" + url)
            ));

            String json = MAPPER.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getAiApiBase() + "/v1/chat/completions"))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode result = MAPPER.readTree(resp.body());
            String summary = result.get("choices").get(0).get("message").get("content").asText().trim();
            log.info("AI 采集摘要成功: url={}", url);
            return summary;
        } catch (Exception e) {
            log.error("AI 采集摘要失败: url={}", url, e);
            throw new RuntimeException("AI 采集摘要失败: " + e.getMessage());
        }
    }

    private String autoSummary(String content) {
        if (content == null || content.isBlank()) return "";
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }
}