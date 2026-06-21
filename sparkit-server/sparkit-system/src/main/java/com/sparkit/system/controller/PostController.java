package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Post;
import com.sparkit.system.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 岗位管理
 */
@Tag(name = "岗位管理", description = "岗位的增删改查")
@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "岗位列表")
    @GetMapping
    public R<PageResult<Post>> list(PageQuery query) {
        return R.ok(postService.page(query));
    }

    @Operation(summary = "获取岗位详情")
    @GetMapping("/{id}")
    public R<Post> getById(@PathVariable Long id) {
        return R.ok(postService.getById(id));
    }

    @Operation(summary = "创建岗位")
    @PostMapping
    public R<?> create(@Valid @RequestBody Post post) {
        postService.save(post);
        return R.ok();
    }

    @Operation(summary = "更新岗位")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Post post) {
        post.setId(id);
        postService.updateById(post);
        return R.ok();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        postService.removeById(id);
        return R.ok();
    }
}