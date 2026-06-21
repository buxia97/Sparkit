package com.sparkit.generator.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.generator.mapper.GenTableColumnMapper;
import com.sparkit.generator.mapper.GenTableMapper;
import com.sparkit.generator.model.entity.GenTable;
import com.sparkit.generator.model.entity.GenTableColumn;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 代码生成器表服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenTableService extends ServiceImpl<GenTableMapper, GenTable> {

    private final GenTableColumnMapper columnMapper;
    private final GenTemplateEngine templateEngine;

    public PageResult<GenTable> page(PageQuery query) {
        IPage<GenTable> page = new Page<>(query.getPage(), query.getPageSize());
        IPage<GenTable> result = lambdaQuery()
                .orderByDesc(GenTable::getCreateTime)
                .page(page);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 生成代码并返回 ZIP 字节数组
     */
    public byte[] generateCode(Long tableId) throws IOException {
        GenTable table = getById(tableId);
        if (table == null) {
            throw new IllegalArgumentException("表不存在: " + tableId);
        }
        List<GenTableColumn> columns = columnMapper.selectList(
                new LambdaQueryWrapper<GenTableColumn>().eq(GenTableColumn::getTableId, tableId)
                        .orderByAsc(GenTableColumn::getSort));
        log.info("开始生成代码: table={} columns={}", table.getTableName(), columns.size());
        return templateEngine.generateAndZip(table, columns);
    }

    /**
     * 预览生成代码
     */
    public java.util.Map<String, String> previewCode(Long tableId) {
        GenTable table = getById(tableId);
        if (table == null) {
            throw new IllegalArgumentException("表不存在: " + tableId);
        }
        List<GenTableColumn> columns = columnMapper.selectList(
                new LambdaQueryWrapper<GenTableColumn>().eq(GenTableColumn::getTableId, tableId)
                        .orderByAsc(GenTableColumn::getSort));
        return templateEngine.preview(table, columns);
    }
}