package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.RoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联 Mapper
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {
}