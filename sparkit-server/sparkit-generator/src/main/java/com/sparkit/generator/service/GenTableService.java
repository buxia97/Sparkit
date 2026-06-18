package com.sparkit.generator.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.generator.mapper.GenTableMapper;
import com.sparkit.generator.model.entity.GenTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 代码生成器表服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenTableService extends ServiceImpl<GenTableMapper, GenTable> {

    public PageResult<GenTable> page(PageQuery query) {
        IPage<GenTable> page = new Page<>(query.getPage(), query.getPageSize());
        IPage<GenTable> result = lambdaQuery()
                .orderByDesc(GenTable::getCreateTime)
                .page(page);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
}