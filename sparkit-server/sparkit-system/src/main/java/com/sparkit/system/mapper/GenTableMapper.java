package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.GenTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 代码生成 Mapper
 */
@Mapper
public interface GenTableMapper extends BaseMapper<GenTable> {

    @Select("SELECT TABLE_NAME AS tableName, TABLE_COMMENT AS tableComment, CREATE_TIME AS createTime " +
            "FROM information_schema.TABLES WHERE TABLE_SCHEMA = (SELECT DATABASE()) " +
            "AND TABLE_NAME NOT LIKE 'sparkit_gen_%'")
    List<Map<String, Object>> selectDbTables();
}