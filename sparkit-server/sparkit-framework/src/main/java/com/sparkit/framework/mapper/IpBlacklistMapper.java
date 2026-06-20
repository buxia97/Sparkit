package com.sparkit.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.framework.model.entity.IpBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * IP 黑名单 Mapper
 */
@Mapper
public interface IpBlacklistMapper extends BaseMapper<IpBlacklist> {
}