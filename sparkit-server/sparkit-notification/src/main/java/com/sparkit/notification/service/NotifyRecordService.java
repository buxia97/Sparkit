package com.sparkit.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.notification.mapper.NotifyRecordMapper;
import com.sparkit.notification.model.entity.NotifyRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 通知记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyRecordService extends ServiceImpl<NotifyRecordMapper, NotifyRecord> {

    public PageResult<NotifyRecord> page(PageQuery query) {
        IPage<NotifyRecord> page = new Page<>(query.getPage(), query.getPageSize());
        IPage<NotifyRecord> result = lambdaQuery()
                .orderByDesc(NotifyRecord::getCreateTime)
                .page(page);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 按类型统计 */
    public List<Map<String, Object>> countByType(String startTime, String endTime) {
        return baseMapper.countByType(startTime, endTime);
    }
}