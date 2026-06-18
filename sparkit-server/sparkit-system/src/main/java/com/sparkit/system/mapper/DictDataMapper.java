package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.DictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字典数据 Mapper
 */
@Mapper
public interface DictDataMapper extends BaseMapper<DictData> {

    @Select("SELECT * FROM sparkit_dict_data WHERE dict_type = #{dictType} AND status = 1 AND deleted = 0 ORDER BY sort")
    List<DictData> selectByDictType(@Param("dictType") String dictType);
}