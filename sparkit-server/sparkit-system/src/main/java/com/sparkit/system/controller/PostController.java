package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.Post;
import com.sparkit.system.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 岗位管理
 */
@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public R<PageResult<Post>> list(PageQuery query) {
        return R.ok(postService.page(query));
    }

    @GetMapping("/{id}")
    public R<Post> getById(@PathVariable Long id) {
        return R.ok(postService.getById(id));
    }

    @PostMapping
    public R<?> create(@Valid @RequestBody Post post) {
        postService.save(post);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody Post post) {
        post.setId(id);
        postService.updateById(post);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        postService.removeById(id);
        return R.ok();
    }
}