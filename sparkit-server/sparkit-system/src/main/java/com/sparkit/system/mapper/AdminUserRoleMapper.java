package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.AdminUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface AdminUserRoleMapper extends BaseMapper<AdminUserRole> {
}