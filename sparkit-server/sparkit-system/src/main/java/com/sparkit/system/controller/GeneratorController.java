package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.mapper.SystemGenTableMapper;
import com.sparkit.system.model.entity.GenTableConfig;
import com.sparkit.system.service.GenTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 代码生成器
 */
@Tag(name = "代码生成器", description = "数据库表代码生成")
@RestController
@RequestMapping("/api/v1/admin/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final GenTableService genTableService;
    private final SystemGenTableMapper genTableMapper;

    @Operation(summary = "已导入的表列表")
    @GetMapping("/tables")
    public R<PageResult<GenTableConfig>> tableList(PageQuery query) {
        return R.ok(genTableService.page(query));
    }

    @Operation(summary = "数据库中的表")
    @GetMapping("/db-tables")
    public R<List<Map<String, Object>>> dbTables() {
        return R.ok(genTableMapper.selectDbTables());
    }

    @Operation(summary = "导入表")
    @PostMapping("/tables/import")
    public R<?> importTables(@RequestBody Map<String, List<String>> params) {
        genTableService.importTables(params.get("tables"));
        return R.ok();
    }

    @Operation(summary = "获取表详情")
    @GetMapping("/tables/{id}")
    public R<GenTableConfig> getTable(@PathVariable Long id) {
        return R.ok(genTableService.getById(id));
    }

    @Operation(summary = "更新表配置")
    @PutMapping("/tables/{id}")
    public R<?> updateTable(@PathVariable Long id, @RequestBody GenTableConfig table) {
        table.setId(id);
        genTableService.updateById(table);
        return R.ok();
    }

    @Operation(summary = "删除表")
    @DeleteMapping("/tables/{id}")
    public R<?> deleteTable(@PathVariable Long id) {
        genTableService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "预览代码")
    @GetMapping("/tables/{id}/preview")
    public R<List<Map<String, String>>> preview(@PathVariable Long id) {
        return R.ok(genTableService.previewCode(id));
    }

    @Operation(summary = "生成代码")
    @PostMapping("/tables/{id}/generate")
    public R<?> generate(@PathVariable Long id) {
        genTableService.generateCode(id);
        return R.ok();
    }
}