package com.sparkit.system.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.system.model.entity.LoginLog;
import com.sparkit.system.model.entity.OperLog;
import com.sparkit.system.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 监控管理
 */
@RestController
@RequestMapping("/api/v1/admin/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/operlog")
    public R<PageResult<OperLog>> operLogList(PageQuery query) {
        return R.ok(monitorService.operLogPage(query));
    }

    @GetMapping("/loginlog")
    public R<PageResult<LoginLog>> loginLogList(PageQuery query) {
        return R.ok(monitorService.loginLogPage(query));
    }
}