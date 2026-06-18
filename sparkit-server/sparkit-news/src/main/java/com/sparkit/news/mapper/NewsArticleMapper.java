package com.sparkit.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.news.model.entity.NewsArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 新闻文章 Mapper
 */
@Mapper
public interface NewsArticleMapper extends BaseMapper<NewsArticle> {

    /** 统计各分类文章数（SQL聚合） */
    List<Map<String, Object>> countByCategory(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /** 统计文章发布趋势（按日期） */
    List<Map<String, Object>> publishTrend(@Param("startTime") String startTime, @Param("endTime") String endTime);
}