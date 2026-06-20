package com.sparkit.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.notification.model.entity.SiteMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内信 Mapper
 */
@Mapper
public interface SiteMessageMapper extends BaseMapper<SiteMessage> {
}