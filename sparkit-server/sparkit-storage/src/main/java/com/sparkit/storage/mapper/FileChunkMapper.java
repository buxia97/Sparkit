package com.sparkit.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.storage.model.entity.FileChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分片 Mapper
 */
@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunk> {
}