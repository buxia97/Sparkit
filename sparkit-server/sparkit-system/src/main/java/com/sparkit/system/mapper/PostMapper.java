package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.Post;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位 Mapper
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {
}