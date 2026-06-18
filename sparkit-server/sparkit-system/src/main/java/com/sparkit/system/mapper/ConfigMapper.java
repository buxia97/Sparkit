package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.Config;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置 Mapper
 */
@Mapper
public interface ConfigMapper extends BaseMapper<Config> {

    /** 批量更新配置值 */
    int batchUpdateByKey(@Param("list") List<Config> configs);
}