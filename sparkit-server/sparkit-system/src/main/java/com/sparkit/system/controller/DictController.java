package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.DictData;
import com.sparkit.system.model.entity.DictType;
import com.sparkit.system.service.DictDataService;
import com.sparkit.system.service.DictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理
 */
@Tag(name = "字典管理", description = "字典类型与字典数据的增删改查")
@RestController
@RequestMapping("/api/v1/admin/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictTypeService dictTypeService;
    private final DictDataService dictDataService;

    // ============ 字典类型 ============

    @Operation(summary = "字典类型列表")
    @GetMapping("/types")
    public R<List<DictType>> typeList() {
        return R.ok(dictTypeService.list());
    }

    @Operation(summary = "获取字典类型详情")
    @GetMapping("/types/{id}")
    public R<DictType> typeGet(@PathVariable Long id) {
        return R.ok(dictTypeService.getById(id));
    }

    @Operation(summary = "创建字典类型")
    @PostMapping("/types")
    public R<?> typeCreate(@Valid @RequestBody DictType dictType) {
        dictTypeService.save(dictType);
        return R.ok();
    }

    @Operation(summary = "更新字典类型")
    @PutMapping("/types/{id}")
    public R<?> typeUpdate(@PathVariable Long id, @Valid @RequestBody DictType dictType) {
        dictType.setId(id);
        dictTypeService.update(dictType);
        return R.ok();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/types/{id}")
    public R<?> typeDelete(@PathVariable Long id) {
        dictTypeService.delete(id);
        return R.ok();
    }

    // ============ 字典数据 ============

    @Operation(summary = "字典数据列表")
    @GetMapping("/data/{dictType}")
    public R<List<DictData>> dataList(@PathVariable String dictType) {
        return R.ok(dictDataService.getByDictType(dictType));
    }

    @Operation(summary = "获取字典数据详情")
    @GetMapping("/data/{id}")
    public R<DictData> dataGet(@PathVariable Long id) {
        return R.ok(dictDataService.getById(id));
    }

    @Operation(summary = "创建字典数据")
    @PostMapping("/data")
    public R<?> dataCreate(@Valid @RequestBody DictData dictData) {
        dictDataService.save(dictData);
        return R.ok();
    }

    @Operation(summary = "更新字典数据")
    @PutMapping("/data/{id}")
    public R<?> dataUpdate(@PathVariable Long id, @Valid @RequestBody DictData dictData) {
        dictData.setId(id);
        dictDataService.update(dictData);
        return R.ok();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/data/{id}")
    public R<?> dataDelete(@PathVariable Long id) {
        dictDataService.delete(id);
        return R.ok();
    }
}