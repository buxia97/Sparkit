package com.sparkit.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.system.mapper.SystemGenTableMapper;
import com.sparkit.system.model.entity.GenTableConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 代码生成服务
 */
@Slf4j
@Service("systemGenTableService")
@RequiredArgsConstructor
public class GenTableService extends ServiceImpl<SystemGenTableMapper, GenTableConfig> {

    private final SystemGenTableMapper genTableMapper;

    public PageResult<GenTableConfig> page(PageQuery query) {
        Page<GenTableConfig> page = page(new Page<>(query.getPage(), query.getPageSize()));
        return PageResult.of(page);
    }

    @Transactional
    public void importTables(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) return;
        List<Map<String, Object>> dbTables = genTableMapper.selectDbTables();
        for (String tableName : tableNames) {
            Map<String, Object> dbTable = dbTables.stream()
                    .filter(t -> tableName.equals(t.get("tableName")))
                    .findFirst().orElse(null);
            if (dbTable == null) continue;

            GenTableConfig genTable = new GenTableConfig();
            genTable.setTableName(tableName);
            genTable.setTableComment((String) dbTable.getOrDefault("tableComment", ""));
            genTable.setClassName(toCamelCase(tableName));
            genTable.setPackageName("com.sparkit");
            genTable.setModuleName("sparkit-" + tableName.split("_")[0]);
            genTable.setAuthor("system");
            save(genTable);
        }
    }

    public List<Map<String, String>> previewCode(Long tableId) {
        GenTableConfig table = getById(tableId);
        if (table == null) return Collections.emptyList();

        List<Map<String, String>> files = new ArrayList<>();
        Map<String, String> entityFile = new HashMap<>();
        entityFile.put("name", table.getClassName() + ".java");
        entityFile.put("content", "// " + table.getClassName() + " entity (preview)\n// Table: " + table.getTableName());
        files.add(entityFile);

        Map<String, String> controllerFile = new HashMap<>();
        controllerFile.put("name", table.getClassName() + "Controller.java");
        controllerFile.put("content", "// " + table.getClassName() + " controller (preview)\n// Table: " + table.getTableName());
        files.add(controllerFile);

        return files;
    }

    public void generateCode(Long tableId) {
        GenTableConfig table = getById(tableId);
        if (table == null) return;
        log.info("开始生成代码: tableName={}, className={}", table.getTableName(), table.getClassName());
        // TODO: 实际代码生成逻辑（模板引擎生成）
    }

    private String toCamelCase(String tableName) {
        // 去掉前缀，如 sparkit_ 等
        String name = tableName.contains("_") ? tableName.substring(tableName.indexOf("_") + 1) : tableName;
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : name.toCharArray()) {
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }
}