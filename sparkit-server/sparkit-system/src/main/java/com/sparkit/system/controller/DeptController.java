package com.sparkit.system.controller;

import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Dept;
import com.sparkit.system.service.DeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理
 */
@RestController
@RequestMapping("/api/v1/admin/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public R<List<Dept>> tree() {
        return R.ok(deptService.getDeptTree());
    }

    @GetMapping
    public R<List<Dept>> list() {
        return R.ok(deptService.list());
    }

    @GetMapping("/{id}")
    public R<Dept> getById(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @PostMapping
    public R<?> create(@Valid @RequestBody Dept dept) {
        deptService.save(dept);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Dept dept) {
        dept.setId(id);
        deptService.updateById(dept);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}