package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    List<Menu> selectMenusByUserId(@Param("userId") Long userId);
}