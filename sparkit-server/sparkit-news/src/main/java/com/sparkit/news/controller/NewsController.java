package com.sparkit.news.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.news.model.entity.NewsArticle;
import com.sparkit.news.model.entity.NewsCategory;
import com.sparkit.news.service.NewsArticleService;
import com.sparkit.news.service.NewsCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 新闻管理
 */
@RestController
@RequestMapping("/api/v1/admin/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsArticleService articleService;
    private final NewsCategoryService categoryService;

    // ============ 分类管理 ============

    @GetMapping("/categories/tree")
    public R<List<NewsCategory>> categoryTree() {
        return R.ok(categoryService.getTree());
    }

    @GetMapping("/categories")
    public R<List<NewsCategory>> categoryList() {
        return R.ok(categoryService.list());
    }

    @GetMapping("/categories/{id}")
    public R<NewsCategory> categoryGet(@PathVariable Long id) {
        return R.ok(categoryService.getById(id));
    }

    @PostMapping("/categories")
    public R<?> categoryCreate(@Valid @RequestBody NewsCategory category) {
        categoryService.save(category);
        return R.ok();
    }

    @PutMapping("/categories/{id}")
    public R<?> categoryUpdate(@PathVariable Long id, @Valid @RequestBody NewsCategory category) {
        category.setId(id);
        categoryService.updateById(category);
        return R.ok();
    }

    @DeleteMapping("/categories/{id}")
    public R<?> categoryDelete(@PathVariable Long id) {
        categoryService.removeById(id);
        return R.ok();
    }

    // ============ 文章管理 ============

    @GetMapping("/articles")
    public R<PageResult<NewsArticle>> articleList(PageQuery query,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) Integer status) {
        return R.ok(articleService.page(query, categoryId, status));
    }

    @GetMapping("/articles/{id}")
    public R<NewsArticle> articleGet(@PathVariable Long id) {
        return R.ok(articleService.getById(id));
    }

    @PostMapping("/articles")
    public R<?> articleCreate(@Valid @RequestBody NewsArticle article) {
        articleService.save(article);
        return R.ok();
    }

    @PutMapping("/articles/{id}")
    public R<?> articleUpdate(@PathVariable Long id, @Valid @RequestBody NewsArticle article) {
        article.setId(id);
        articleService.updateById(article);
        return R.ok();
    }

    @DeleteMapping("/articles/{id}")
    public R<?> articleDelete(@PathVariable Long id) {
        articleService.removeById(id);
        return R.ok();
    }

    @PutMapping("/articles/{id}/publish")
    public R<?> articlePublish(@PathVariable Long id) {
        articleService.publish(id);
        return R.ok();
    }

    @PutMapping("/articles/{id}/unpublish")
    public R<?> articleUnpublish(@PathVariable Long id) {
        articleService.unpublish(id);
        return R.ok();
    }

    // ============ 统计 ============

    @GetMapping("/statistics/category-count")
    public R<List<Map<String, Object>>> categoryCount(@RequestParam(required = false) String startTime,
                                                       @RequestParam(required = false) String endTime) {
        return R.ok(articleService.countByCategory(startTime, endTime));
    }

    @GetMapping("/statistics/publish-trend")
    public R<List<Map<String, Object>>> publishTrend(@RequestParam(required = false) String startTime,
                                                      @RequestParam(required = false) String endTime) {
        return R.ok(articleService.publishTrend(startTime, endTime));
    }
}