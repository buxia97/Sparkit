package com.sparkit.news.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.news.model.entity.NewsComment;
import com.sparkit.news.service.NewsCommentService;
import com.sparkit.news.service.NewsAiService;
import com.sparkit.news.service.NewsArticleService;
import com.sparkit.news.service.NewsSeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 新闻评论、SEO、AI 控制器
 */
@RestController
@RequestMapping("/api/v1/admin/news")
@RequiredArgsConstructor
public class NewsExtendController {

    private final NewsCommentService commentService;
    private final NewsSeoService seoService;
    private final NewsAiService aiService;
    private final NewsArticleService articleService;

    // ========== 评论管理 ==========

    @GetMapping("/comments")
    public R<PageResult<NewsComment>> commentList(PageQuery query,
                                                   @RequestParam(required = false) Long articleId,
                                                   @RequestParam(required = false) Integer status) {
        return R.ok(commentService.page(query, articleId, status));
    }

    @GetMapping("/articles/{articleId}/comments")
    public R<List<NewsComment>> articleComments(@PathVariable Long articleId) {
        return R.ok(commentService.listByArticleId(articleId));
    }

    @PostMapping("/comments")
    public R<?> createComment(@RequestBody NewsComment comment) {
        commentService.create(comment);
        return R.ok();
    }

    @PutMapping("/comments/{id}/audit")
    public R<?> auditComment(@PathVariable Long id, @RequestParam Integer status) {
        commentService.audit(id, status);
        return R.ok();
    }

    @DeleteMapping("/comments/{id}")
    public R<?> deleteComment(@PathVariable Long id) {
        commentService.removeById(id);
        return R.ok();
    }

    // ========== SEO ==========

    @GetMapping("/sitemap")
    public String sitemap(@RequestParam(defaultValue = "https://sparkit.com") String baseUrl) {
        return seoService.generateSitemap(baseUrl);
    }

    // ========== AI 功能 ==========

    @PostMapping("/ai/summarize/{articleId}")
    public R<?> generateSummary(@PathVariable Long articleId) {
        var article = articleService.getById(articleId);
        return R.ok(aiService.generateSummary(article));
    }

    @PostMapping("/ai/generate")
    public R<?> generateContent(@RequestBody Map<String, Object> params) {
        String topic = (String) params.get("topic");
        String style = (String) params.getOrDefault("style", "正式新闻");
        int wordCount = params.containsKey("wordCount") ? ((Number) params.get("wordCount")).intValue() : 500;
        return R.ok(aiService.generateContent(topic, style, wordCount));
    }

    @PostMapping("/ai/collect")
    public R<?> collectSummary(@RequestBody Map<String, String> params) {
        String url = params.get("url");
        return R.ok(aiService.collectSummary(url));
    }
}