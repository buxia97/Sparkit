package com.sparkit.news.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.framework.security.SecurityContextHolder;
import com.sparkit.news.mapper.NewsCommentMapper;
import com.sparkit.news.model.entity.NewsComment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 新闻评论服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCommentService extends ServiceImpl<NewsCommentMapper, NewsComment> {

    public PageResult<NewsComment> page(PageQuery query, Long articleId, Integer status) {
        IPage<NewsComment> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<NewsComment> wrapper = new LambdaQueryWrapper<>();
        if (articleId != null) {
            wrapper.eq(NewsComment::getArticleId, articleId);
        }
        if (status != null) {
            wrapper.eq(NewsComment::getStatus, status);
        }
        wrapper.orderByDesc(NewsComment::getCreateTime);
        IPage<NewsComment> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<NewsComment> listByArticleId(Long articleId) {
        return lambdaQuery().eq(NewsComment::getArticleId, articleId)
                .eq(NewsComment::getStatus, 1)
                .orderByAsc(NewsComment::getCreateTime)
                .list();
    }

    @Transactional
    public void create(NewsComment comment) {
        comment.setUserId(SecurityContextHolder.getUserId());
        comment.setUserName(SecurityContextHolder.getUsername());
        comment.setStatus(1);
        save(comment);
        log.info("评论创建成功: articleId={} userId={}", comment.getArticleId(), comment.getUserId());
    }

    @Transactional
    public void audit(Long id, Integer status) {
        NewsComment comment = getById(id);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        comment.setStatus(status);
        updateById(comment);
    }

    public long countByArticleId(Long articleId) {
        return lambdaQuery().eq(NewsComment::getArticleId, articleId)
                .eq(NewsComment::getStatus, 1).count();
    }
}