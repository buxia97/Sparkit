package com.sparkit.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.storage.mapper.StorageConfigMapper;
import com.sparkit.storage.model.entity.StorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 存储源配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageConfigService extends ServiceImpl<StorageConfigMapper, StorageConfig> {

    @Transactional
    @Override
    public boolean save(StorageConfig config) {
        // 如果设置为默认，先将其他默认取消
        if (config.getIsDefault() != null && config.getIsDefault() == 1) {
            StorageConfig update = new StorageConfig();
            update.setIsDefault(0);
            update(update, new LambdaQueryWrapper<StorageConfig>()
                    .eq(StorageConfig::getIsDefault, 1));
        }
        return super.save(config);
    }

    @Transactional
    @Override
    public boolean updateById(StorageConfig config) {
        if (config.getIsDefault() != null && config.getIsDefault() == 1) {
            StorageConfig update = new StorageConfig();
            update.setIsDefault(0);
            update(update, new LambdaQueryWrapper<StorageConfig>()
                    .eq(StorageConfig::getIsDefault, 1)
                    .ne(StorageConfig::getId, config.getId()));
        }
        return super.updateById(config);
    }

    /** 分页查询 */
    public PageResult<StorageConfig> page(PageQuery query) {
        Page<StorageConfig> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<StorageConfig> wrapper = new LambdaQueryWrapper<StorageConfig>()
                .orderByAsc(StorageConfig::getSort)
                .orderByDesc(StorageConfig::getCreateTime);
        page(page, wrapper);
        return PageResult.of(page);
    }

    /** 获取默认存储源 */
    public StorageConfig getDefault() {
        return getOne(new LambdaQueryWrapper<StorageConfig>()
                .eq(StorageConfig::getIsDefault, 1)
                .eq(StorageConfig::getStatus, 1));
    }
}