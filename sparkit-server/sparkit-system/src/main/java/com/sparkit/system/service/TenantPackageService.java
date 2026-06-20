package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.system.mapper.TenantPackageMapper;
import com.sparkit.system.model.entity.TenantPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户套餐服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPackageService extends ServiceImpl<TenantPackageMapper, TenantPackage> {

    public PageResult<TenantPackage> page(PageQuery query) {
        IPage<TenantPackage> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<TenantPackage> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null) {
            wrapper.like(TenantPackage::getPackageName, query.getKeyword());
        }
        wrapper.orderByAsc(TenantPackage::getSort).orderByDesc(TenantPackage::getCreateTime);
        IPage<TenantPackage> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<TenantPackage> listEnabled() {
        return lambdaQuery().eq(TenantPackage::getStatus, 1).list();
    }

    @Transactional
    public void create(TenantPackage pkg) {
        if (lambdaQuery().eq(TenantPackage::getPackageCode, pkg.getPackageCode()).count() > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "套餐编码已存在");
        }
        save(pkg);
    }

    @Transactional
    public void update(TenantPackage pkg) {
        TenantPackage exist = getById(pkg.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        updateById(pkg);
    }
}