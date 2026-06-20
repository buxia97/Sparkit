package com.sparkit.search.controller;

import com.sparkit.common.model.R;
import com.sparkit.search.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 搜索引擎管理
 */
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchService esService;

    @GetMapping("/indices")
    public R<?> listIndices() {
        return R.ok(esService.listIndices());
    }

    @PostMapping("/indices/{indexName}")
    public R<?> createIndex(@PathVariable String indexName,
                            @RequestParam(defaultValue = "3") int shards,
                            @RequestParam(defaultValue = "1") int replicas) {
        return R.ok(esService.createIndex(indexName, shards, replicas) ? "创建成功" : "创建失败");
    }

    @DeleteMapping("/indices/{indexName}")
    public R<?> deleteIndex(@PathVariable String indexName) {
        return R.ok(esService.deleteIndex(indexName) ? "删除成功" : "删除失败");
    }

    @GetMapping("/search")
    public R<?> search(@RequestParam String index,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int pageSize,
                       @RequestParam(defaultValue = "title,content") String fields) {
        String[] fieldArr = fields.split(",");
        return R.ok(esService.search(index, keyword, page, pageSize, fieldArr));
    }

    @GetMapping("/suggest")
    public R<?> suggest(@RequestParam String index,
                        @RequestParam String keyword,
                        @RequestParam(defaultValue = "title") String field,
                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(esService.suggest(index, keyword, field, size));
    }

    @PostMapping("/documents")
    public R<?> indexDocument(@RequestParam String index,
                              @RequestParam String docId,
                              @RequestBody Map<String, Object> document) {
        return R.ok(esService.indexDocument(index, docId, document) ? "索引成功" : "索引失败");
    }

    @DeleteMapping("/documents")
    public R<?> deleteDocument(@RequestParam String index, @RequestParam String docId) {
        return R.ok(esService.deleteDocument(index, docId) ? "删除成功" : "删除失败");
    }

    @PostMapping("/reindex")
    public R<?> reindex(@RequestParam String sourceIndex, @RequestParam String targetIndex) {
        return R.ok(esService.reindex(sourceIndex, targetIndex) ? "重建成功" : "重建失败");
    }
}