package com.sparkit.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.news.model.entity.NewsArticleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章标签关联 Mapper
 */
@Mapper
public interface NewsArticleTagMapper extends BaseMapper<NewsArticleTag> {
}