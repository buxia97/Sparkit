package com.sparkit.generator.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.generator.model.entity.GenTable;
import com.sparkit.generator.service.GenTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 代码生成器
 */
@RestController
@RequestMapping("/api/v1/admin/generator")
@RequiredArgsConstructor
public class GenController {

    private final GenTableService genTableService;

    @GetMapping("/tables")
    public R<PageResult<GenTable>> tableList(PageQuery query) {
        return R.ok(genTableService.page(query));
    }

    @GetMapping("/tables/{id}")
    public R<GenTable> tableGet(@PathVariable Long id) {
        return R.ok(genTableService.getById(id));
    }

    @PostMapping("/tables")
    public R<?> tableCreate(@RequestBody GenTable table) {
        genTableService.save(table);
        return R.ok();
    }

    @PutMapping("/tables/{id}")
    public R<?> tableUpdate(@PathVariable Long id, @RequestBody GenTable table) {
        table.setId(id);
        genTableService.updateById(table);
        return R.ok();
    }

    @DeleteMapping("/tables/{id}")
    public R<?> tableDelete(@PathVariable Long id) {
        genTableService.removeById(id);
        return R.ok();
    }
}