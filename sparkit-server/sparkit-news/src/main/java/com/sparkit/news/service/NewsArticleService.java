package com.sparkit.news.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.news.mapper.NewsArticleMapper;
import com.sparkit.news.model.entity.NewsArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 新闻文章服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleService extends ServiceImpl<NewsArticleMapper, NewsArticle> {

    public PageResult<NewsArticle> page(PageQuery query, Long categoryId, Integer status) {
        IPage<NewsArticle> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<NewsArticle> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(NewsArticle::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(NewsArticle::getStatus, status);
        }
        if (query.getKeyword() != null) {
            wrapper.and(w -> w.like(NewsArticle::getTitle, query.getKeyword())
                    .or().like(NewsArticle::getSummary, query.getKeyword()));
        }
        wrapper.orderByDesc(NewsArticle::getIsTop)
                .orderByDesc(NewsArticle::getPublishTime);
        IPage<NewsArticle> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void publish(Long id) {
        NewsArticle article = getById(id);
        if (article != null) {
            article.setStatus(1);
            article.setPublishTime(LocalDateTime.now());
            updateById(article);
        }
    }

    @Transactional
    public void unpublish(Long id) {
        NewsArticle article = getById(id);
        if (article != null) {
            article.setStatus(0);
            updateById(article);
        }
    }

    /** 增加浏览量 */
    public void incrementView(Long id) {
        update(new LambdaUpdateWrapper<NewsArticle>()
                .eq(NewsArticle::getId, id)
                .setSql("view_count = view_count + 1"));
    }

    /** 统计各分类文章数 */
    public List<Map<String, Object>> countByCategory(String startTime, String endTime) {
        return baseMapper.countByCategory(startTime, endTime);
    }

    /** 发布趋势 */
    public List<Map<String, Object>> publishTrend(String startTime, String endTime) {
        return baseMapper.publishTrend(startTime, endTime);
    }
}