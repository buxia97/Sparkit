package com.sparkit.generator.controller;

import com.sparkit.common.model.R;
import com.sparkit.generator.service.GenTableService;
import com.sparkit.generator.service.GenTemplateEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 代码生成器
 */
@Tag(name = "代码生成（旧版）", description = "代码下载 ZIP")
@RestController
@RequestMapping("/api/v1/admin/generator")
@RequiredArgsConstructor
public class GenController {

    private final GenTableService genTableService;
    private final GenTemplateEngine templateEngine;

    /**
     * 生成代码并下载 ZIP
     */
    @Operation(summary = "下载生成代码 ZIP")
    @GetMapping("/tables/{tableId}/generate")
    public ResponseEntity<byte[]> generateCode(@PathVariable Long tableId) throws IOException {
        byte[] zipBytes = genTableService.generateCode(tableId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "sparkit_code.zip");
        return ResponseEntity.ok().headers(headers).body(zipBytes);
    }
}