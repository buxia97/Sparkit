package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Dept;
import com.sparkit.system.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理
 */
@Tag(name = "部门管理", description = "部门树的增删改查")
@RestController
@RequestMapping("/api/v1/admin/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    public R<List<Dept>> tree() {
        return R.ok(deptService.getDeptTree());
    }

    @Operation(summary = "部门列表")
    @GetMapping
    public R<List<Dept>> list() {
        return R.ok(deptService.list());
    }

    @Operation(summary = "获取部门详情")
    @GetMapping("/{id}")
    public R<Dept> getById(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @Operation(summary = "创建部门")
    @PostMapping
    public R<?> create(@Valid @RequestBody Dept dept) {
        deptService.save(dept);
        return R.ok();
    }

    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Dept dept) {
        dept.setId(id);
        deptService.updateById(dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}