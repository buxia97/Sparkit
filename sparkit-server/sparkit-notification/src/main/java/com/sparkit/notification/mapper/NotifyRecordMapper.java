package com.sparkit.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.notification.model.entity.NotifyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 通知记录 Mapper
 */
@Mapper
public interface NotifyRecordMapper extends BaseMapper<NotifyRecord> {

    /** 按通知类型统计发送量 */
    List<Map<String, Object>> countByType(@Param("startTime") String startTime, @Param("endTime") String endTime);
}