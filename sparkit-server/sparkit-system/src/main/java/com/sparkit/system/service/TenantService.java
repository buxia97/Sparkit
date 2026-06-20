package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.system.mapper.TenantMapper;
import com.sparkit.system.model.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService extends ServiceImpl<TenantMapper, Tenant> {

    public PageResult<Tenant> page(PageQuery query) {
        IPage<Tenant> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null) {
            wrapper.like(Tenant::getTenantName, query.getKeyword())
                    .or().like(Tenant::getTenantCode, query.getKeyword())
                    .or().like(Tenant::getContactName, query.getKeyword());
        }
        wrapper.orderByAsc(Tenant::getSort).orderByDesc(Tenant::getCreateTime);
        IPage<Tenant> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<Tenant> listEnabled() {
        return lambdaQuery().eq(Tenant::getStatus, 1)
                .ge(Tenant::getExpireTime, LocalDateTime.now())
                .list();
    }

    @Transactional
    public void create(Tenant tenant) {
        if (lambdaQuery().eq(Tenant::getTenantCode, tenant.getTenantCode()).count() > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "租户编码已存在");
        }
        save(tenant);
    }

    @Transactional
    public void update(Tenant tenant) {
        Tenant exist = getById(tenant.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        updateById(tenant);
    }

    @Transactional
    public void changeStatus(Long id, Integer status) {
        Tenant tenant = getById(id);
        if (tenant == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        tenant.setStatus(status);
        updateById(tenant);
    }

    public Tenant getByCode(String tenantCode) {
        return lambdaQuery().eq(Tenant::getTenantCode, tenantCode).one();
    }
}