package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.system.mapper.LoginLogMapper;
import com.sparkit.system.mapper.OperLogMapper;
import com.sparkit.system.model.entity.LoginLog;
import com.sparkit.system.model.entity.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 监控服务
 */
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final OperLogMapper operLogMapper;
    private final LoginLogMapper loginLogMapper;

    public PageResult<OperLog> operLogPage(PageQuery query) {
        Page<OperLog> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<OperLog> wrapper = new LambdaQueryWrapper<OperLog>()
                .orderByDesc(OperLog::getOperTime);
        operLogMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    public void cleanOperLog() {
        operLogMapper.delete(new LambdaQueryWrapper<>());
    }

    public PageResult<LoginLog> loginLogPage(PageQuery query) {
        Page<LoginLog> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .orderByDesc(LoginLog::getLoginTime);
        loginLogMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    public void cleanLoginLog() {
        loginLogMapper.delete(new LambdaQueryWrapper<>());
    }
}