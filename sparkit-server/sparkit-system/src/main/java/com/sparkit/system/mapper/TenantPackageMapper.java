package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.TenantPackage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户套餐 Mapper
 */
@Mapper
public interface TenantPackageMapper extends BaseMapper<TenantPackage> {
}