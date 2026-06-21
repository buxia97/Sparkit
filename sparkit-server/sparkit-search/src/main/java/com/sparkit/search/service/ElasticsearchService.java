package com.sparkit.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sparkit.common.model.PageResult;
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
 * Elasticsearch 搜索引擎服务
 * 通过 HTTP API 调用 Elasticsearch，实现全文检索、索引管理、搜索建议
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConfigService configService;

    private String getEsUrl() {
        return configService.getConfigValue("search.elasticsearch.url", "http://localhost:9200");
    }

    // ============ 索引管理 ============

    public boolean createIndex(String indexName, int shards, int replicas) {
        try {
            ObjectNode settings = MAPPER.createObjectNode();
            settings.put("number_of_shards", shards);
            settings.put("number_of_replicas", replicas);

            ObjectNode body = MAPPER.createObjectNode();
            body.set("settings", settings);

            String url = getEsUrl() + "/" + indexName;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode result = MAPPER.readTree(resp.body());
            boolean acknowledged = result.has("acknowledged") && result.get("acknowledged").asBoolean();
            log.info("创建ES索引: {} acknowledged={}", indexName, acknowledged);
            return acknowledged;
        } catch (Exception e) {
            log.error("创建ES索引失败: {}", indexName, e);
            return false;
        }
    }

    public boolean deleteIndex(String indexName) {
        try {
            String url = getEsUrl() + "/" + indexName;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode result = MAPPER.readTree(resp.body());
            boolean acknowledged = result.has("acknowledged") && result.get("acknowledged").asBoolean();
            log.info("删除ES索引: {} acknowledged={}", indexName, acknowledged);
            return acknowledged;
        } catch (Exception e) {
            log.error("删除ES索引失败: {}", indexName, e);
            return false;
        }
    }

    public boolean indexExists(String indexName) {
        try {
            String url = getEsUrl() + "/" + indexName;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<Void> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> listIndices() {
        try {
            String url = getEsUrl() + "/_cat/indices?format=json";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode arr = MAPPER.readTree(resp.body());
            List<String> indices = new ArrayList<>();
            for (JsonNode node : arr) {
                if (node.has("index")) {
                    indices.add(node.get("index").asText());
                }
            }
            return indices;
        } catch (Exception e) {
            log.error("获取ES索引列表失败", e);
            return Collections.emptyList();
        }
    }

    // ============ 文档管理 ============

    public boolean indexDocument(String indexName, String docId, Map<String, Object> document) {
        try {
            String body = MAPPER.writeValueAsString(document);
            String url = getEsUrl() + "/" + indexName + "/_doc/" + docId;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode result = MAPPER.readTree(resp.body());
            String resultType = result.has("result") ? result.get("result").asText() : "unknown";
            log.info("ES索引文档: index={} docId={} result={}", indexName, docId, resultType);
            return "created".equals(resultType) || "updated".equals(resultType);
        } catch (Exception e) {
            log.error("ES索引文档失败: index={} docId={}", indexName, docId, e);
            return false;
        }
    }

    public boolean deleteDocument(String indexName, String docId) {
        try {
            String url = getEsUrl() + "/" + indexName + "/_doc/" + docId;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("ES删除文档: index={} docId={} status={}", indexName, docId, resp.statusCode());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.error("ES删除文档失败: index={} docId={}", indexName, docId, e);
            return false;
        }
    }

    // ============ 全文搜索 ============

    public Map<String, Object> search(String indexName, String keyword, int page, int pageSize, String... fields) {
        try {
            ObjectNode query = MAPPER.createObjectNode();

            if (keyword != null && !keyword.isBlank() && fields.length > 0) {
                ObjectNode multiMatch = MAPPER.createObjectNode();
                ObjectNode multiMatchQuery = MAPPER.createObjectNode();
                multiMatchQuery.put("query", keyword);
                ArrayNode fieldsArr = MAPPER.createArrayNode();
                for (String f : fields) fieldsArr.add(f);
                multiMatchQuery.set("fields", fieldsArr);
                multiMatch.set("multi_match", multiMatchQuery);
                query.set("query", multiMatch);
            } else {
                ObjectNode matchAll = MAPPER.createObjectNode();
                matchAll.set("match_all", MAPPER.createObjectNode());
                query.set("query", matchAll);
            }

            query.put("from", (page - 1) * pageSize);
            query.put("size", pageSize);

            ObjectNode highlight = MAPPER.createObjectNode();
            ObjectNode fieldsHighlight = MAPPER.createObjectNode();
            for (String f : fields) {
                fieldsHighlight.set(f, MAPPER.createObjectNode());
            }
            highlight.set("fields", fieldsHighlight);
            highlight.put("pre_tags", "<em>");
            highlight.put("post_tags", "</em>");
            query.set("highlight", highlight);

            String url = getEsUrl() + "/" + indexName + "/_search";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(query.toString(), StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode result = MAPPER.readTree(resp.body());

            long total = result.get("hits").get("total").get("value").asLong();
            JsonNode hits = result.get("hits").get("hits");
            List<Map<String, Object>> docs = new ArrayList<>();
            for (JsonNode hit : hits) {
                Map<String, Object> doc = MAPPER.convertValue(hit.get("_source"), Map.class);
                doc.put("_score", hit.get("_score").asDouble());
                if (hit.has("highlight")) {
                    doc.put("_highlight", MAPPER.convertValue(hit.get("highlight"), Map.class));
                }
                docs.add(doc);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("total", total);
            response.put("list", docs);
            response.put("page", page);
            response.put("pageSize", pageSize);
            return response;
        } catch (Exception e) {
            log.error("ES搜索失败: index={} keyword={}", indexName, keyword, e);
            Map<String, Object> empty = new HashMap<>();
            empty.put("total", 0);
            empty.put("list", Collections.emptyList());
            return empty;
        }
    }

    // ============ 搜索建议 ============

    public List<String> suggest(String indexName, String keyword, String field, int size) {
        try {
            ObjectNode suggestNode = MAPPER.createObjectNode();
            ObjectNode textSuggest = MAPPER.createObjectNode();
            textSuggest.put("prefix", keyword);
            ObjectNode completion = MAPPER.createObjectNode();
            completion.put("field", field + ".suggest");
            completion.put("size", size);
            textSuggest.set("completion", completion);
            suggestNode.set("text-suggest", textSuggest);

            ObjectNode body = MAPPER.createObjectNode();
            body.set("suggest", suggestNode);

            String url = getEsUrl() + "/" + indexName + "/_search";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode result = MAPPER.readTree(resp.body());

            List<String> suggestions = new ArrayList<>();
            JsonNode suggestResults = result.get("suggest").get("text-suggest");
            if (suggestResults != null) {
                for (JsonNode item : suggestResults) {
                    JsonNode options = item.get("options");
                    if (options != null) {
                        for (JsonNode opt : options) {
                            suggestions.add(opt.get("text").asText());
                        }
                    }
                }
            }
            return suggestions;
        } catch (Exception e) {
            log.error("ES搜索建议失败: index={} keyword={}", indexName, keyword, e);
            return Collections.emptyList();
        }
    }

    // ============ 批量操作 ============

    public int bulkIndex(String indexName, List<Map<String, Object>> documents) {
        int count = 0;
        for (Map<String, Object> doc : documents) {
            String docId = doc.containsKey("id") ? doc.get("id").toString() : UUID.randomUUID().toString();
            if (indexDocument(indexName, docId, doc)) count++;
        }
        return count;
    }

    public boolean reindex(String sourceIndex, String targetIndex) {
        try {
            ObjectNode source = MAPPER.createObjectNode();
            source.put("index", sourceIndex);
            ObjectNode dest = MAPPER.createObjectNode();
            dest.put("index", targetIndex);
            ObjectNode body = MAPPER.createObjectNode();
            body.set("source", source);
            body.set("dest", dest);

            String url = getEsUrl() + "/_reindex";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("ES reindex: {} -> {} status={}", sourceIndex, targetIndex, resp.statusCode());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.error("ES reindex失败: {} -> {}", sourceIndex, targetIndex, e);
            return false;
        }
    }
}