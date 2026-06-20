package com.sparkit.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.search.model.entity.SearchIndex;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索索引 Mapper
 */
@Mapper
public interface SearchIndexMapper extends BaseMapper<SearchIndex> {
}