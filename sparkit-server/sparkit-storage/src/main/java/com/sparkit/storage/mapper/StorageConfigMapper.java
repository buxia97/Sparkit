package com.sparkit.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.storage.model.entity.StorageConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StorageConfigMapper extends BaseMapper<StorageConfig> {
}