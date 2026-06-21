package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.service.ImportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 数据导入导出
 */
@Tag(name = "数据导入导出", description = "CSV/Excel 数据导入导出")
@RestController
@RequestMapping("/api/v1/admin/import-export")
@RequiredArgsConstructor
public class ImportExportController {

    private final ImportExportService importExportService;

    /**
     * 导出数据
     * @param module 模块名：user/role/dept/config 等
     * @param format 导出格式：csv/excel
     */
    @Operation(summary = "导出数据")
    @PostMapping("/export/{module}")
    public void export(@PathVariable String module,
                       @RequestParam(defaultValue = "csv") String format,
                       @RequestBody Map<String, Object> params,
                       HttpServletResponse response) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) params.get("headers");
        @SuppressWarnings("unchecked")
        List<List<String>> data = (List<List<String>>) params.get("data");
        String fileName = (String) params.getOrDefault("fileName", module);

        if ("excel".equals(format)) {
            importExportService.exportExcel(fileName, headers, data, response);
        } else {
            importExportService.exportCsv(fileName, headers, data, response);
        }
    }

    /**
     * 下载导入模板
     */
    @Operation(summary = "下载导入模板")
    @GetMapping("/template/{module}")
    public void downloadTemplate(@PathVariable String module,
                                 @RequestParam List<String> headers,
                                 @RequestParam(defaultValue = "导入模板") String fileName,
                                 HttpServletResponse response) throws IOException {
        importExportService.downloadTemplate(fileName, headers, module, response);
    }

    /**
     * 文件导入（解析 CSV）
     */
    @Operation(summary = "导入 CSV 文件")
    @PostMapping("/import/{module}")
    public R<List<String[]>> importFile(@PathVariable String module,
                                         @RequestParam("file") MultipartFile file) throws IOException {
        List<String[]> rows = importExportService.parseCsv(file);
        return R.ok(rows);
    }
}