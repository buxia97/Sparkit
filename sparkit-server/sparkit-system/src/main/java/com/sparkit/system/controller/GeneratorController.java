package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.mapper.SystemGenTableConfigMapper;
import com.sparkit.system.model.entity.GenTableConfigConfig;
import com.sparkit.system.service.GenTableConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 代码生成器
 */
@RestController
@RequestMapping("/api/v1/admin/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final GenTableConfigService genTableService;
    private final SystemGenTableConfigMapper genTableMapper;

    @GetMapping("/tables")
    public R<PageResult<GenTableConfig>> tableList(PageQuery query) {
        return R.ok(genTableService.page(query));
    }

    @GetMapping("/db-tables")
    public R<List<Map<String, Object>>> dbTables() {
        return R.ok(genTableMapper.selectDbTables());
    }

    @PostMapping("/tables/import")
    public R<?> importTables(@RequestBody Map<String, List<String>> params) {
        genTableService.importTables(params.get("tables"));
        return R.ok();
    }

    @GetMapping("/tables/{id}")
    public R<GenTableConfig> getTable(@PathVariable Long id) {
        return R.ok(genTableService.getById(id));
    }

    @PutMapping("/tables/{id}")
    public R<?> updateTable(@PathVariable Long id, @RequestBody GenTableConfig table) {
        table.setId(id);
        genTableService.updateById(table);
        return R.ok();
    }

    @DeleteMapping("/tables/{id}")
    public R<?> deleteTable(@PathVariable Long id) {
        genTableService.removeById(id);
        return R.ok();
    }

    @GetMapping("/tables/{id}/preview")
    public R<List<Map<String, String>>> preview(@PathVariable Long id) {
        return R.ok(genTableService.previewCode(id));
    }

    @PostMapping("/tables/{id}/generate")
    public R<?> generate(@PathVariable Long id) {
        genTableService.generateCode(id);
        return R.ok();
    }
}