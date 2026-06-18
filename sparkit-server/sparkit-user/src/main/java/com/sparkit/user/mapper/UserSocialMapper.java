package com.sparkit.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.user.model.entity.UserSocial;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户社交绑定 Mapper
 */
@Mapper
public interface UserSocialMapper extends BaseMapper<UserSocial> {
}