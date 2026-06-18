package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门 Mapper
 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {
}