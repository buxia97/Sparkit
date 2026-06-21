package com.sparkit.search.controller;

import com.sparkit.common.model.R;
import com.sparkit.search.service.ElasticsearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 搜索引擎管理
 */
@Tag(name = "搜索引擎", description = "ES 索引管理、搜索、建议")
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchService esService;

    @Operation(summary = "索引列表")
    @GetMapping("/indices")
    public R<?> listIndices() {
        return R.ok(esService.listIndices());
    }

    @Operation(summary = "创建索引")
    @PostMapping("/indices/{indexName}")
    public R<?> createIndex(@PathVariable String indexName,
                            @RequestParam(defaultValue = "3") int shards,
                            @RequestParam(defaultValue = "1") int replicas) {
        return R.ok(esService.createIndex(indexName, shards, replicas) ? "创建成功" : "创建失败");
    }

    @Operation(summary = "删除索引")
    @DeleteMapping("/indices/{indexName}")
    public R<?> deleteIndex(@PathVariable String indexName) {
        return R.ok(esService.deleteIndex(indexName) ? "删除成功" : "删除失败");
    }

    @Operation(summary = "搜索")
    @GetMapping("/search")
    public R<?> search(@RequestParam String index,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int pageSize,
                       @RequestParam(defaultValue = "title,content") String fields) {
        String[] fieldArr = fields.split(",");
        return R.ok(esService.search(index, keyword, page, pageSize, fieldArr));
    }

    @Operation(summary = "搜索建议")
    @GetMapping("/suggest")
    public R<?> suggest(@RequestParam String index,
                        @RequestParam String keyword,
                        @RequestParam(defaultValue = "title") String field,
                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(esService.suggest(index, keyword, field, size));
    }

    @Operation(summary = "索引文档")
    @PostMapping("/documents")
    public R<?> indexDocument(@RequestParam String index,
                              @RequestParam String docId,
                              @RequestBody Map<String, Object> document) {
        return R.ok(esService.indexDocument(index, docId, document) ? "索引成功" : "索引失败");
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/documents")
    public R<?> deleteDocument(@RequestParam String index, @RequestParam String docId) {
        return R.ok(esService.deleteDocument(index, docId) ? "删除成功" : "删除失败");
    }

    @Operation(summary = "重建索引")
    @PostMapping("/reindex")
    public R<?> reindex(@RequestParam String sourceIndex, @RequestParam String targetIndex) {
        return R.ok(esService.reindex(sourceIndex, targetIndex) ? "重建成功" : "重建失败");
    }
}