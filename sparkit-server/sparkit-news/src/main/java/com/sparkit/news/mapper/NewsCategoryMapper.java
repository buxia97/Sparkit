package com.sparkit.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.news.model.entity.NewsCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新闻分类 Mapper
 */
@Mapper
public interface NewsCategoryMapper extends BaseMapper<NewsCategory> {
}