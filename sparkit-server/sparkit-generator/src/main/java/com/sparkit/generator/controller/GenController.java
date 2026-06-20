package com.sparkit.generator.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.generator.model.entity.GenTable;
import com.sparkit.generator.service.GenTableService;
import com.sparkit.generator.service.GenTemplateEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 代码生成器
 */
@RestController
@RequestMapping("/api/v1/admin/generator")
@RequiredArgsConstructor
public class GenController {

    private final GenTableService genTableService;
    private final GenTemplateEngine templateEngine;

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

    /**
     * 生成代码并下载 ZIP
     */
    @GetMapping("/tables/{tableId}/generate")
    public ResponseEntity<byte[]> generateCode(@PathVariable Long tableId) throws IOException {
        byte[] zipBytes = genTableService.generateCode(tableId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "sparkit_code.zip");
        return ResponseEntity.ok().headers(headers).body(zipBytes);
    }

    /** 预览生成结果 */
    @GetMapping("/tables/{tableId}/preview")
    public R<Map<String, String>> preview(@PathVariable Long tableId) {
        return R.ok(genTableService.previewCode(tableId));
    }
}