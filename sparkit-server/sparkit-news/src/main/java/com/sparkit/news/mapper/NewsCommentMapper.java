package com.sparkit.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.news.model.entity.NewsComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 新闻评论 Mapper
 */
@Mapper
public interface NewsCommentMapper extends BaseMapper<NewsComment> {

    @Select("SELECT COUNT(*) FROM sparkit_news_comment WHERE article_id = #{articleId} AND status = 1")
    long countByArticleId(Long articleId);
}